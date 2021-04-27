package com.cdpjenkins.genetic.model

import com.cdpjenkins.genetic.image.grabPixels
import com.cdpjenkins.genetic.image.writePng
import com.cdpjenkins.genetic.svg.SvgRenderer
import json.JSON
import java.awt.image.BufferedImage
import java.io.File

class Evolver(var individual: Individual, masterImage: BufferedImage) {
    private var listeners: MutableList<EvolverListener> = mutableListOf()
    private val masterPixels = grabPixels(masterImage)

    fun mutate() {
        val newIndividual = individual.mutate()
        newIndividual.drawAndCalculateFitness(masterPixels)
        if (newIndividual.fitness < individual.fitness) {
            individual = newIndividual
            println("new fitness: ${individual.fitness} genome size: ${individual.genome.size}")
            listeners.forEach{ it(individual) }
        }
    }

    fun addListener(listener: EvolverListener) {
        this.listeners.add(listener)
    }
}

fun makeEvolver(
    masterImage: BufferedImage,
    initialIndividual: Individual?
): Evolver {
    val boundsRectangle = BoundsRectangle(0, 0, masterImage.width, masterImage.height)
    val individual = initialIndividual ?: makeIndividual(masterImage, boundsRectangle)
    val evolver = Evolver(individual, masterImage)
    return evolver
}

fun Individual.saveToDisk() {
    if (generation % 10 == 0) {
        val outputFile =
            File(String.format("output/png/cow_%010d.png", generation))
        writePng(this, outputFile)

        JSON().serialiseToFile(
            File(
                String.format(
                    "output/json/cow_%010d.json",
                    generation
                )
            ), this
        )

        SvgRenderer().render(
            File(String.format("output/svg/cow_%010d.svg", generation)),
            this
        )
    }
}

typealias EvolverListener = (Individual) -> Unit

