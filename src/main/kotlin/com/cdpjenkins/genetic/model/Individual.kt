package com.cdpjenkins.genetic.model

import ADD_SHAPE_CHANCE
import com.cdpjenkins.genetic.image.grabPixels
import com.fasterxml.jackson.annotation.JsonIgnore
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.system.measureTimeMillis

data class Individual(
    val genome: List<Shape>,
    val bounds: BoundsRectangle,
    val generation: Int = 1
) {
    var timeInMillis: Long = 0
    val createdTimestamp = System.currentTimeMillis()

    @JsonIgnore
    var bufferedImage: BufferedImage = BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB)
    var fitness = Integer.MAX_VALUE

    fun draw(g: Graphics2D) {
        g.setColor(Color.BLACK)
        g.fillRect(bounds.minX, bounds.minY, bounds.maxX, bounds.maxY)

        for (shape in genome) {
            shape.draw(g)
        }
    }

    fun drawToBuffer() {
        val g = bufferedImage.createGraphics()
        draw(g)
    }

    fun drawAndCalculateFitness(masterPixels: IntArray) {
        val timeInMillis = measureTimeMillis {
            drawToBuffer()
            fitness = calculateFitness(masterPixels)
        }
        this.timeInMillis = timeInMillis
    }

    private fun calculateFitness(masterPixels: IntArray): Int {
        val pixels = grabPixels(bufferedImage)
        val pixelDistance = comparePixels(masterPixels, pixels)

        return pixelDistance
    }

    private fun comparePixels(masterPixels: IntArray, pixels: IntArray): Int {
        if (masterPixels.size != pixels.size) {
            throw IllegalArgumentException("Oh my!")
        }

        var total = 0
        for ((i, pi) in pixels.withIndex()) {
            val mpi = masterPixels[i]

            val pr: Int  =  (pi shr 16) and 0xFF
            val pg: Int  =  (pi shr 8 ) and 0xFF
            val pb: Int  =  (pi       ) and 0xFF
            val mpr: Int = (mpi shr 16) and 0xFF
            val mpg: Int = (mpi shr 8 ) and 0xFF
            val mpb: Int = (mpi       ) and 0xFF

            val dr: Int = pr - mpr
            val dg: Int = pg - mpg
            val db: Int = pb - mpb

            total += ((dr*dr + dg*dg + db*db) shr 6)
        }

        return total
    }

    fun mutate(): Individual {
        val newGenome = genome
            .map { it.maybeMutate() }

        // Urgh this is freaking horrible
        val newNewGenome =
            if (randint(0, ADD_SHAPE_CHANCE) == 0) {
                newGenome + spawnRandomShape(bounds)
            } else {
                newGenome
            }

        val newIndividual = Individual(
            newNewGenome,
            bounds,
            generation + 1
        )

        return newIndividual
    }

    fun describe(): String {
        val size = genome.size
        val time = timeInMillis
        val generation = generation
        val fitness = fitness
        val description =
            "Genome size: $size Time: $time Generation: $generation Fitness: $fitness"
        return description
    }
}
