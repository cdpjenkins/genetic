package com.cdpjenkins.genetic.model

import EvolverSettings
import com.cdpjenkins.genetic.image.grabPixels
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.image.BufferedImage
import java.io.File

class Evolver(
    val name: String,
    var individual: Individual,
    masterImage: BufferedImage,
    val settings: EvolverSettings
) {
    private val logger = KotlinLogging.logger {}

    private var listeners: MutableList<EvolverListener> = mutableListOf()
    private val masterPixels: IntArray = grabPixels(masterImage)

    @Synchronized fun mutateOnce() {
        val newIndividual = individual.mutate(settings)
        newIndividual.drawAndCalculateFitness(masterPixels)
        if (newIndividual.fitness < individual.fitness) {
            individual = newIndividual
            logger.info { individual.describe(name) }

//            individual.drawDiff(masterPixels)

            listeners.forEach{ it.notify(individual) }
        }
    }

    @Synchronized fun addListener(listener: EvolverListener) {
        logger.info { "Adding listener ${listener.describe()}" }

        this.listeners.add(listener)
    }

}

fun makeEvolver(
    name: String,
    masterImage: BufferedImage,
    initialIndividual: Individual?,
    evolverSettings: EvolverSettings
): Evolver {
    val boundsRectangle = BoundsRectangle(0, 0, masterImage.width, masterImage.height)
    val individual = initialIndividual ?: makeIndividual(boundsRectangle, evolverSettings)
    val evolver = Evolver(name, individual, masterImage, evolverSettings)
    return evolver
}

interface EvolverListener {
    fun notify(individual: Individual)
    fun describe(): String = this.javaClass.simpleName
}

fun ensureDirExists(dirName: String) {
    File(dirName).also {
        if (!it.exists()) it.mkdir()
    }
}