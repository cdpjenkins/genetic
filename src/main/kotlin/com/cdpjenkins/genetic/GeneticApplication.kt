package com.cdpjenkins.genetic

import EvolverSettings
import com.cdpjenkins.genetic.dudestore.client.DudeStoreClient
import com.cdpjenkins.genetic.model.Evolver
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.persistence.S3Client
import com.cdpjenkins.genetic.ui.GUI
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO

class GeneticApplication(
    val name: String,
    val evolver: Evolver
) {

    fun start() {
        evolver.start()
    }

    companion object {
        fun create(name: String, secret: String, masterImageFileName: String): GeneticApplication {

            val dudeClient = DudeStoreClient("https://genetic-dude.herokuapp.com", name, secret)
            val s3Client = S3Client(name)

            logger.info("Creating evolver for name {}", name)
            val maybeInitialIndividual = dudeClient.getLatestDude()

            val evolverSettings = EvolverSettings(name)
            val masterImage = ImageIO.read(File(masterImageFileName).toURI().toURL())
            val evolver = makeEvolver(masterImage, maybeInitialIndividual, evolverSettings)

            if (!GraphicsEnvironment.isHeadless()) {
                val gui = GUI(masterImage)
                evolver.addListener { gui.updateUiWithNewIndividual(it) }
                gui.isVisible = true
            }

            // evolver.addListener { it.saveToDisk(name) }
            evolver.addListener { dudeClient.postDude(it) }
            evolver.addListener { s3Client.saveToS3(it); }

            val geneticApplication = GeneticApplication(name, evolver)

            return geneticApplication
        }
    }
}