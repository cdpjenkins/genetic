package com.cdpjenkins.genetic

import MASTER_IMAGE_FILE
import com.cdpjenkins.genetic.json.JSON
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.model.saveToDisk
import com.cdpjenkins.genetic.ui.GUI
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO


fun main(args: Array<String>) {

    val initialIndividual = if (args.size == 1) {
        JSON().deserialiseFromFile(File(args[0]))
    } else {
        null
    }

    val masterImage = ImageIO.read(File(MASTER_IMAGE_FILE))
    val evolver = makeEvolver(masterImage, initialIndividual)

    if (!GraphicsEnvironment.isHeadless()) {
        val gui = GUI(masterImage, evolver)
        gui.isVisible = true
    }

    evolver.addListener { it.saveToDisk() }

    evolver.start()
}
