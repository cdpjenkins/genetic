package com.cdpjenkins.genetic.model

import EvolverSettings
import com.cdpjenkins.genetic.image.grabPixels
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.File

class Evolver(
    var individual: Individual,
    masterImage: BufferedImage,
    val settings: EvolverSettings
) {
    private val logger: Logger = LoggerFactory.getLogger(Evolver::class.java)

    private var listeners: MutableList<EvolverListener> = mutableListOf()
    private val masterPixels: IntArray = grabPixels(masterImage)

    init {
        ensureDirExists("output")
        ensureDirExists("output/png")
        ensureDirExists("output/json")
        ensureDirExists("output/svg")
    }

    @Synchronized fun mutate() {
        val newIndividual = individual.mutate(settings)
        newIndividual.drawAndCalculateFitness(masterPixels)
        if (newIndividual.fitness < individual.fitness) {
            individual = newIndividual
            logger.info(individual.describe())

//            individual.drawDiff(masterPixels)

            listeners.forEach{ it.notify(individual) }
        }
    }

    @Synchronized fun addListener(listener: EvolverListener) {
        this.listeners.add(listener)
    }

    @Synchronized
    fun start() {
        val runnableMeDo = object : Runnable {
            override fun run() {
                while (true) {
                    mutate()
                }
            }
        }
        val threadMeDo = Thread(runnableMeDo)
        threadMeDo.start()
    }
}

fun makeEvolver(
    masterImage: BufferedImage,
    initialIndividual: Individual?,
    evolverSettings: EvolverSettings
): Evolver {
    val boundsRectangle = BoundsRectangle(0, 0, masterImage.width, masterImage.height)
    val individual = initialIndividual ?: makeIndividual(boundsRectangle, evolverSettings)
    val evolver = Evolver(individual, masterImage, evolverSettings)
    return evolver
}

fun interface EvolverListener {
    fun notify(individual: Individual)
}

fun ensureDirExists(dirName: String) {
    File(dirName).also {
        if (!it.exists()) it.mkdir()
    }
}