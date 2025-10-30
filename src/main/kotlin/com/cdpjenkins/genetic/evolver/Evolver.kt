package com.cdpjenkins.genetic.evolver

import com.cdpjenkins.genetic.image.grabPixels
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.image.BufferedImage

private val logger = KotlinLogging.logger {}

class Evolver(
    val name: String,
    var individual: Individual,
    masterImage: BufferedImage,
    val mutator: Mutator
) {

    private var listeners: MutableList<EvolverListener> = mutableListOf()
    private val masterPixels: IntArray = grabPixels(masterImage)

    @Synchronized fun mutateOnce() {
        val newIndividual = mutator.mutate(individual)
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
    evolverSettings: EvolverSettings,
    mutator: Mutator
): Evolver {
    val boundsRectangle = BoundsRectangle(0, 0, masterImage.width, masterImage.height)
    val individual = initialIndividual ?: makeIndividual(boundsRectangle, evolverSettings)
    val evolver = Evolver(name, individual, masterImage, mutator)
    return evolver
}

interface EvolverListener {
    fun notify(individual: Individual)
    fun describe(): String = this.javaClass.simpleName
}
