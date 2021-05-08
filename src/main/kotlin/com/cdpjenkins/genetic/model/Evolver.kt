package com.cdpjenkins.genetic.model

import com.cdpjenkins.genetic.image.grabPixels
import com.cdpjenkins.genetic.image.writePng
import com.cdpjenkins.genetic.json.JSON
import com.cdpjenkins.genetic.svg.SvgRenderer
import java.awt.image.BufferedImage
import java.io.File

class Evolver(var individual: Individual, masterImage: BufferedImage) {
    private var listeners: MutableList<EvolverListener> = mutableListOf()
    private val masterPixels = grabPixels(masterImage)

    init {
        ensureDirExists("output")
        ensureDirExists("output/png")
        ensureDirExists("output/json")
        ensureDirExists("output/svg")
    }

    @Synchronized fun mutate() {
        val newIndividual = individual.mutate()
        newIndividual.drawAndCalculateFitness(masterPixels)
        if (newIndividual.fitness < individual.fitness) {
            individual = newIndividual
            println(individual.describe())
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

        SvgRenderer().renderToFile(
            File(String.format("output/svg/cow_%010d.svg", generation)),
            this
        )
    }
}

fun interface EvolverListener {
    fun notify(individual: Individual)
}

fun ensureDirExists(dirName: String) {
    val dir = File(dirName)
    if (!dir.exists()) dir.mkdir()
}