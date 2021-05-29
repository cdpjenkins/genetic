package com.cdpjenkins.genetic

import MASTER_IMAGE_FILE
import com.cdpjenkins.genetic.dudestore.client.DudeClient
import com.cdpjenkins.genetic.json.deserialiseFromFile
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.model.saveToDisk
import com.cdpjenkins.genetic.ui.GUI
import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO


fun main(args: Array<String>) {
    val secret = System.getenv("SECRET")

    val dudeClient = DudeClient("https://genetic-dude.herokuapp.com", "steve", secret)

    val initialIndividual = if (args.size == 1) {
        deserialiseFromFile(File(args[0]))
    } else {
        dudeClient.getLatestDude()
    }

    val masterImage = ImageIO.read(File(MASTER_IMAGE_FILE))
    val evolver = makeEvolver(masterImage, initialIndividual)

    if (!GraphicsEnvironment.isHeadless()) {
        val gui = GUI(masterImage, evolver)
        gui.isVisible = true
    }

    evolver.addListener { it.saveToDisk() }

    evolver.addListener {
        dudeClient.postDude(it)
    }

    evolver.start()
}
