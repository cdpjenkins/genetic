package com.cdpjenkins.genetic.evolver

import com.cdpjenkins.genetic.dudestore.client.BlockingDudeStoreClient
import com.cdpjenkins.genetic.dudestore.client.NonBlockingDudeStoreClient
import com.cdpjenkins.genetic.json.deserialiseFromFile
import com.cdpjenkins.genetic.json.serialiseToFile
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.evolver.persistence.FilePersister
import com.cdpjenkins.genetic.evolver.persistence.S3Persister
import com.cdpjenkins.genetic.ui.GUI
import com.cdpjenkins.genetic.ui.GUIEvolverListener
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame

class GeneticEvolverApplication(
    val name: String,
    val evolver: Evolver
) {

    fun start() {
        Thread {
            while (true) {
                evolver.mutateOnce()
            }
        }.apply {
            start()
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        fun create(name: String, secret: String, masterImageFileName: String?): GeneticEvolverApplication {

            val blockingDudeStoreClient = BlockingDudeStoreClient("https://genetic-dude.herokuapp.com", secret)
            val dudeStoreClient = NonBlockingDudeStoreClient(
                blockingDudeStoreClient
            )

            val settings = readSettingsOrDefault(name, blockingDudeStoreClient)
            serialiseToFile(File("written-evolver-settings-${name}.json"), settings)

            val masterImage = readMasterImage(name, masterImageFileName, blockingDudeStoreClient)

            logger.info { "Creating evolver for $name" }
            val maybeInitialIndividual = dudeStoreClient.getLatestDude(name)
            if (maybeInitialIndividual != null) {
                logger.info {
                    val generation = maybeInitialIndividual.generation
                    val fitness = maybeInitialIndividual.fitness
                    val size = maybeInitialIndividual.genome.size
                    "Found initial individual with generation: $generation fitness: $fitness genomeSize: $size"
                }
            }

            val evolver = makeEvolver(
                name,
                masterImage,
                maybeInitialIndividual,
                settings,
                Mutator(settings),
            )

            if (!GraphicsEnvironment.isHeadless()) {
                val gui = GUI(masterImage)
                evolver.addListener(GUIEvolverListener(gui, name))
                gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
                gui.isVisible = true
            }

//            evolver.addListener(FilePersistenceListener(name, FilePersister()))
            evolver.addListener(DudeStoreClientListener(name, dudeStoreClient))
            if (settings.saveToS3 == true) {
                evolver.addListener(S3Persister.create(name))
            }

            val geneticEvolverApplication = GeneticEvolverApplication(name, evolver)

            return geneticEvolverApplication
        }

        private fun readMasterImage(
            name: String,
            masterImageFileNameParameter: String?,
            blockingDudeStoreClient: BlockingDudeStoreClient
        ): BufferedImage {
            return if (masterImageFileNameParameter != null) {
                logger.info { "Loading master image from file" }
                ImageIO.read(File(masterImageFileNameParameter).toURI().toURL())
            } else {
                val masterImageBytesFromDudeStore = blockingDudeStoreClient.getMasterImage(name)
                if (masterImageBytesFromDudeStore != null) {
                    logger.info { "Loading master image from DudeStore" }
                    ImageIO.read(masterImageBytesFromDudeStore.inputStream())
                } else {
                    throw RuntimeException("No file specified and no master image found in DudeStore")
                }
            }
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

class DudeStoreClientListener(val name: String, val dudeStoreClient: NonBlockingDudeStoreClient) : EvolverListener {
    override fun notify(individual: Individual) {
        dudeStoreClient.postDude(individual, name)
    }
}

class FilePersistenceListener(val name: String, val persister: FilePersister) : EvolverListener {
    override fun notify(individual: Individual) {
        persister.saveToDisk( individual, name)
    }
}