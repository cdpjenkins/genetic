package com.cdpjenkins.genetic

import EvolverSettings
import com.cdpjenkins.genetic.dudestore.client.DudeStoreClient
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.persistence.S3Client
import com.cdpjenkins.genetic.ui.GUI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO

val logger: Logger = LoggerFactory.getLogger(object{}::class.java)

fun main(args: Array<String>) {
    val secret = System.getenv("SECRET")

    val name = args[0]

    // TODO
    // - Pull out the three things here into their own methods
    // - Extract class GeneticEvolverApplication

    logger.info("Creating evolver for name {}", name)

    val dudeClient = DudeStoreClient("https://genetic-dude.herokuapp.com", name, secret)
    val s3Client = S3Client(name)

    val initialIndividual = dudeClient.getLatestDude()

    val evolverSettings = EvolverSettings(name)
    val masterImage = ImageIO.read(File(evolverSettings.masterImageFile).toURI().toURL())
    val evolver = makeEvolver(masterImage, initialIndividual, evolverSettings)

    if (!GraphicsEnvironment.isHeadless()) {
        val gui = GUI(masterImage, evolver)
        evolver.addListener { gui.updateUiWithNewIndividual(it) }
        gui.isVisible = true
    }

//    evolver.addListener { it.saveToDisk(name) }
    evolver.addListener { dudeClient.postDude(it) }
    evolver.addListener { s3Client.saveToS3(it); }

    evolver.start()
}
