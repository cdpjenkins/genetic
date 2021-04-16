package com.cdpjenkins.genetic.model

import java.awt.image.BufferedImage

class Evolver(var individual: Individual) {
    private var listeners: MutableList<EvolverListener> = mutableListOf()

    fun mutate() {
        val newIndividual = individual.mutate()
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
    width: Int,
    height: Int,
    bufferedImage: BufferedImage
): Evolver {
    val individual = makeIndividual(width, height, bufferedImage)
    val evolver = Evolver(individual)
    return evolver
}

typealias EvolverListener = (Individual) -> Unit

