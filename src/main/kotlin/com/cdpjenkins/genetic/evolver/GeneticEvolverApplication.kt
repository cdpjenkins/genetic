package com.cdpjenkins.genetic.evolver

import com.cdpjenkins.genetic.dudestore.client.DudeStoreClient
import com.cdpjenkins.genetic.evolver.persistence.FilePersister
import com.cdpjenkins.genetic.evolver.persistence.S3PersistenceListener
import com.cdpjenkins.genetic.evolver.persistence.S3Persister
import com.cdpjenkins.genetic.json.deserialiseFromFile
import com.cdpjenkins.genetic.json.serialiseToFile
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.ui.GUI
import com.cdpjenkins.genetic.ui.GUIEvolverListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.core.Status
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.swing.JFrame

class GeneticEvolverApplication(
    val name: String,
    val evolver: Evolver
) {
    var shouldDownloadNewIndividual: Boolean = false

    fun start() {
        Thread {
            while (true) {
                evolver.mutateOnce()
            }
        }.apply {
            start()
        }
    }

    @Synchronized
    fun shouldDownloadNewIndividual(): Boolean {
        return shouldDownloadNewIndividual
    }

    @Synchronized
    fun notifyShouldDownloadNewIndividual() {
        logger.info { "notifyShouldDownloadNewIndividual()" }
        shouldDownloadNewIndividual = true
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        fun create(name: String, secret: String, masterImageFileName: String?): GeneticEvolverApplication {
            val dudeStoreClient = DudeStoreClient("https://genetic-dude.herokuapp.com", secret)

            val settings = readSettingsOrDefault(name, dudeStoreClient)
            serialiseToFile(File("written-evolver-settings-${name}.json"), settings)

            val masterImage = readMasterImage(name, masterImageFileName, dudeStoreClient)

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

            val geneticEvolverApplication = GeneticEvolverApplication(name, evolver)

            if (!GraphicsEnvironment.isHeadless()) {
                val gui = GUI(masterImage)
                evolver.addListener(GUIEvolverListener(gui, name))
                gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
                gui.isVisible = true
            }

            if (settings.saveToFilesystem == true) {
                evolver.addListener(FilePersistenceListener(name, FilePersister(name)))
            }
            evolver.addListener(
                DudeStoreClientListener(name, dudeStoreClient) {
                    logger.error{ "we hit a failure: $it" }
                    geneticEvolverApplication.notifyShouldDownloadNewIndividual()
                }
            )
            if (settings.saveToS3 == true) {
                evolver.addListener(S3PersistenceListener(S3Persister.create(name)))
            }

            return geneticEvolverApplication
        }

        private fun readMasterImage(
            name: String,
            masterImageFileNameParameter: String?,
            dudeStoreClient: DudeStoreClient
        ): BufferedImage {
            return if (masterImageFileNameParameter != null) {
                logger.info { "Loading master image from file" }
                ImageIO.read(File(masterImageFileNameParameter).toURI().toURL())
            } else {
                val masterImageBytesFromDudeStore = dudeStoreClient.getMasterImage(name)
                if (masterImageBytesFromDudeStore != null) {
                    logger.info { "Loading master image from DudeStore" }
                    ImageIO.read(masterImageBytesFromDudeStore.inputStream())
                } else {
                    throw RuntimeException("No file specified and no master image found in DudeStore")
                }
            }
        }

        private fun readSettingsOrDefault(name: String, dudeStoreClient: DudeStoreClient): EvolverSettings {
            return tryReadingSettingsFromFile(name)
                ?: tryReadingSettingsFromDudeStore(name, dudeStoreClient)
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

        private fun tryReadingSettingsFromDudeStore(name: String, dudeStoreClient: DudeStoreClient): EvolverSettings? {
            val settings = dudeStoreClient.getSettings(name)
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

class DudeStoreClientListener(
    val name: String,
    val dudeStoreClient: DudeStoreClient,
    val conflictListener: DudeStoreClientFailureListener
) : EvolverListener {
    val logger = KotlinLogging.logger {}

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r).apply { isDaemon = true }
    }

    override fun notify(individual: Individual) {
        executorService.submit({
            try {
                val status = dudeStoreClient.postDude(individual, name)

                if (!status.successful) {
                    conflictListener.onFailure(status)
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to post dude" }
            }
        })
    }
}

fun interface DudeStoreClientFailureListener {
    fun onFailure(status: Status)
}

class FilePersistenceListener(val name: String, val persister: FilePersister) : EvolverListener {
    override fun notify(individual: Individual) {
        persister.saveToDisk(individual)
    }
}