package com.cdpjenkins.genetic.model

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber
import java.io.File
import javax.imageio.ImageIO

class Individual(
    var genome: List<Circle>,
    val masterImage: BufferedImage,
    val width: Int,
    val height: Int,
    var generation: Int = 1
) {
    var bufferedImage: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
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
        g.fillRect(0, 0, width, height)

        draw(g)
    }

    fun drawAndCalculateFitness() {
        drawToBuffer()
        fitness = calculateFitness()
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
        val pixels = IntArray(width * height)
        val pixelGrabber = PixelGrabber(image, 0, 0, width, height, pixels, 0, width)
        pixelGrabber.grabPixels()
        return pixels
    }

    fun mutate() {
        val newGenome = genome.map { it.mutate() }
        val newIndividual = Individual(newGenome, masterImage, width, height, generation + 1)
        newIndividual.drawAndCalculateFitness()

        this.generation++

        if (newIndividual.fitness < this.fitness) {
            this.genome = newIndividual.genome
            this.drawAndCalculateFitness()
            println("new fitness: ${this.fitness}")

            val outputFile = File(String.format("output/cow_%010d.png", newIndividual.generation))
            ImageIO.write(newIndividual.bufferedImage, "png", outputFile)
        }
    }
}
