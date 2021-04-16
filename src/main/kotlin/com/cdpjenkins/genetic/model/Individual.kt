package com.cdpjenkins.genetic.model

import com.cdpjenkins.genetic.image.grabPixels
import com.fasterxml.jackson.annotation.JsonIgnore
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.system.measureTimeMillis

class Individual(
    var genome: List<Shape>,
    val bounds: BoundsRectangle,
    var generation: Int = 1
) {
    var timeInMillis: Long = 0

    @JsonIgnore
    var bufferedImage: BufferedImage = BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB)
    var fitness = Integer.MAX_VALUE

    fun draw(g: Graphics2D) {
        for (shape in genome) {
            shape.draw(g)
        }
    }

    fun drawToBuffer() {
        val g = bufferedImage.createGraphics()
        g.setColor(Color.BLACK)
        g.fillRect(bounds.minX, bounds.minY, bounds.maxX, bounds.maxY)

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
        val newGenome = genome.map { it.maybeMutate() }
        val newIndividual = Individual(
            newGenome,
            bounds,
            generation + 1
        )

        return newIndividual
    }
}
