package com.cdpjenkins.genetic.model

import com.cdpjenkins.genetic.image.grabPixels
import java.awt.image.BufferedImage

class Evolver(var individual: Individual, masterImage: BufferedImage) {
    private var listeners: MutableList<EvolverListener> = mutableListOf()
    private val masterPixels = grabPixels(masterImage)

    fun mutate() {
        val newIndividual = individual.mutate()
        newIndividual.drawAndCalculateFitness(masterPixels)
        if (newIndividual.fitness < individual.fitness) {
            individual = newIndividual
            println("new fitness: ${individual.fitness}")

            listeners.forEach{ it(individual) }
        }
    }

    fun addListener(listener: EvolverListener) {
        this.listeners.add(listener)
    }
}

fun makeEvolver(
    boundsRectangle: BoundsRectangle,
    masterImage: BufferedImage,
    initialIndividual: Individual?
): Evolver {
    val individual = initialIndividual ?: makeIndividual(masterImage, boundsRectangle)
    val evolver = Evolver(individual, masterImage)
    return evolver
}

typealias EvolverListener = (Individual) -> Unit

