package com.cdpjenkins.genetic

import com.cdpjenkins.genetic.dudestore.client.DudeStoreClient
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.persistence.S3Client
import com.cdpjenkins.genetic.ui.GUI
import evolverSettings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO


val logger: Logger = LoggerFactory.getLogger(object{}::class.java)

fun main(args: Array<String>) {
    val secret = System.getenv("SECRET")

    val name = args[0]

    logger.info("Creating evolver for name {}", name)

    val dudeClient = DudeStoreClient("https://genetic-dude.herokuapp.com", name, secret)
    val s3Client = S3Client(name)

    val initialIndividual = dudeClient.getLatestDude()

    val masterImage = ImageIO.read(File(evolverSettings.masterImageFile).toURI().toURL())
    val evolver = makeEvolver(masterImage, initialIndividual)

    if (!GraphicsEnvironment.isHeadless()) {
        val gui = GUI(masterImage, evolver)
        gui.isVisible = true
    }

//    evolver.addListener { it.saveToDisk(name) }
    evolver.addListener { dudeClient.postDude(it) }
    evolver.addListener { s3Client.saveToS3(it); }

    evolver.start()
}
