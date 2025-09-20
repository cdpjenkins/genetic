package com.cdpjenkins.genetic

import EvolverSettings
import com.cdpjenkins.genetic.dudestore.client.BlockingDudeStoreClient
import com.cdpjenkins.genetic.dudestore.client.NonBlockingDudeStoreClient
import com.cdpjenkins.genetic.json.deserialiseFromFile
import com.cdpjenkins.genetic.json.serialiseToFile
import com.cdpjenkins.genetic.model.Evolver
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.persistence.S3Client
import com.cdpjenkins.genetic.ui.GUI
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class GeneticEvolverApplication(
    val name: String,
    val evolver: Evolver
) {

    fun start() {
        evolver.start()
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        fun create(name: String, secret: String, masterImageFileName: String): GeneticEvolverApplication {

            val blockingDudeStoreClient = BlockingDudeStoreClient("https://genetic-dude.herokuapp.com", secret)
            val dudeStoreClient = NonBlockingDudeStoreClient(
                blockingDudeStoreClient
            )
            val s3Client = S3Client(name)

            val settings = readSettingsOrDefault(name, blockingDudeStoreClient)
            serialiseToFile(File("written-evolver-settings-${name}.json"), settings)

            logger.info { "${"Creating evolver for name {}"} $name" }
            val maybeInitialIndividual = dudeStoreClient.getLatestDude(name)
            if (maybeInitialIndividual != null) {
                logger.info {
                    val generation = maybeInitialIndividual.generation
                    val fitness = maybeInitialIndividual.fitness
                    val size = maybeInitialIndividual.genome.size
                    "Found initial individual with generation: $generation fitness: $fitness genomeSize: $size"
                }
            }

            val masterImageBytesFromDudeStore = blockingDudeStoreClient.getMasterImage(name)
            val masterImage = readMasterImage(masterImageBytesFromDudeStore, masterImageFileName)
            val evolver = makeEvolver(
                masterImage, maybeInitialIndividual, settings)

            if (!GraphicsEnvironment.isHeadless()) {
                val gui = GUI(masterImage)
                evolver.addListener { gui.updateUiWithNewIndividual(it) }
                gui.isVisible = true
            }

            // evolver.addListener { it.saveToDisk(name) }
            evolver.addListener { dudeStoreClient.postDude(it, name) }
            evolver.addListener { s3Client.saveToS3(it); }

            val geneticEvolverApplication = GeneticEvolverApplication(name, evolver)

            return geneticEvolverApplication
        }

        private fun readMasterImage(
            masterImageBytesFromDudeStore: ByteArray?,
            masterImageFileName: String
        ): BufferedImage = if (masterImageBytesFromDudeStore != null) {
            logger.info { "Found master image in DudeStore" }
            ImageIO.read(masterImageBytesFromDudeStore.inputStream())
        } else {
            logger.info { "No master image found in DudeStore, loading from file" }
            ImageIO.read(File(masterImageFileName).toURI().toURL())
        }

        private fun readSettingsOrDefault(name: String, blockingDudeStoreClient: BlockingDudeStoreClient): EvolverSettings {
            return tryReadingSettingsFromFile(name)
                ?: tryReadingSettingsFromDudeStore(name, blockingDudeStoreClient)
                ?: defaultSettings(name)
        }

        private fun tryReadingSettingsFromFile(name: String): EvolverSettings? {
            return if (File("evolver-settings-${name}.json").exists()) {
                val settings = deserialiseFromFile<EvolverSettings>(File("evolver-settings-${name}.json"))!!
                logger.info { "Evolver settings from file: $settings" }
                settings
            } else {
                null
            }
        }

        private fun tryReadingSettingsFromDudeStore(name: String, blockingDudeStoreClient: BlockingDudeStoreClient): EvolverSettings? {
            val settings = blockingDudeStoreClient.getSettings(name)
            logger.info { "Evolver settings from DudeStore: ${settings}" }
            return settings
        }

        private fun defaultSettings(name: String): EvolverSettings {
            val settings = EvolverSettings.default(name)
            logger.info { "Using default evolver settings: ${settings}" }
            return settings
        }

    }
}
