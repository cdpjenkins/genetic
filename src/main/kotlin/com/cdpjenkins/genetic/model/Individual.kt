package com.cdpjenkins.genetic.model

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber
import kotlin.system.measureTimeMillis

class Individual(
    var genome: List<Shape>,
    val masterImage: BufferedImage,
    val bounds: BoundsRectangle,
    var generation: Int = 1
) {
    var timeInMillis: Long = 0
    var bufferedImage: BufferedImage = BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB)
    var fitness = Integer.MAX_VALUE
    val masterPixels = grabPixels(masterImage)

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

    fun drawAndCalculateFitness() {
        val timeInMillis = measureTimeMillis {
            drawToBuffer()
            fitness = calculateFitness()
        }
        this.timeInMillis = timeInMillis
    }

    private fun calculateFitness(): Int {
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

    private fun grabPixels(image: BufferedImage): IntArray {
        val pixels = IntArray(bounds.width * bounds.height)
        val pixelGrabber = PixelGrabber(image, bounds.minX, bounds.minY, bounds.maxX, bounds.maxY, pixels, 0, bounds.width)
        pixelGrabber.grabPixels()
        return pixels
    }

    fun mutate(): Individual {
        val newGenome = genome.map { it.maybeMutate() }
        val newIndividual = Individual(
            newGenome,
            masterImage,
            bounds,
            generation + 1
        )
        newIndividual.drawAndCalculateFitness()

        return newIndividual
    }
}
