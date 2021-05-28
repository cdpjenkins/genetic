package com.cdpjenkins.genetic

import MASTER_IMAGE_FILE
import com.cdpjenkins.genetic.json.deserialiseFromFile
import com.cdpjenkins.genetic.json.deserialiseIndividual
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.model.saveToDisk
import com.cdpjenkins.genetic.ui.GUI
import org.http4k.asString
import org.http4k.client.OkHttp
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson.auto
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO


fun main(args: Array<String>) {
    val initialIndividual = if (args.size == 1) {
        deserialiseFromFile(File(args[0]))
    } else {
        val response =
            OkHttp()(Request(Method.GET, "https://genetic-dude.herokuapp.com/dude/steve?type=json"))
        println(response)

        val individualLens = Body.auto<Individual>().toLens()
        individualLens(response)
    }

    val masterImage = ImageIO.read(File(MASTER_IMAGE_FILE))
    val evolver = makeEvolver(masterImage, initialIndividual)

    if (!GraphicsEnvironment.isHeadless()) {
        val gui = GUI(masterImage, evolver)
        gui.isVisible = true
    }

    evolver.addListener { it.saveToDisk() }

    evolver.addListener {
        val secret = System.getenv("SECRET")
        val response = OkHttp()(
            Request(Method.POST, "https://genetic-dude.herokuapp.com/dude/steve?secret=$secret")
                .body(serialise(it))
        )
        println(response)

    }

    evolver.start()
}
