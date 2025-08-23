package com.cdpjenkins.genetic

import EvolverSettings
import com.cdpjenkins.genetic.dudestore.client.DudeStoreClient
import com.cdpjenkins.genetic.json.deserialiseFromFile
import com.cdpjenkins.genetic.json.serialiseToFile
import com.cdpjenkins.genetic.model.Evolver
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.persistence.S3Client
import com.cdpjenkins.genetic.ui.GUI
import io.github.oshai.kotlinlogging.KotlinLogging
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
        private val logger = KotlinLogging.logger {}

        fun create(name: String, secret: String, masterImageFileName: String): GeneticApplication {

            val settings = readSettingsOrDefault(name)

            serialiseToFile(File("written-evolver-settings.json"), settings)

            val dudeClient = DudeStoreClient("https://genetic-dude.herokuapp.com", name, secret)
            val s3Client = S3Client(name)

            logger.info { "${"Creating evolver for name {}"} $name" }
            val maybeInitialIndividual = dudeClient.getLatestDude()
            if (maybeInitialIndividual != null) {
                logger.info {
                    val generation = maybeInitialIndividual.generation
                    val fitness = maybeInitialIndividual.fitness
                    val size = maybeInitialIndividual.genome.size
                    "Found initial individual with generation: $generation fitness: $fitness genomeSize: $size"
                }
            }

            val masterImage = ImageIO.read(File(masterImageFileName).toURI().toURL())
            val evolver = makeEvolver(
                masterImage, maybeInitialIndividual, settings)

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

        private fun readSettingsOrDefault(name: String): EvolverSettings {
            return if (File("evolver-settings.json").exists()) {
                val settings = deserialiseFromFile<EvolverSettings>(File("evolver-settings.json"))!!
                logger.info { "Loaded evolver settings: $settings" }
                settings
            } else {
                val settings = EvolverSettings.default(name)
                logger.info { "Using default evolver settings: ${settings}" }
                settings
            }
        }
    }
}
