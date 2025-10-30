package com.cdpjenkins.genetic.playpen

import EvolverSettings
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import com.cdpjenkins.genetic.evolver.spawnRandomStrokedCubicCurveShape
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import java.awt.*
import javax.swing.*

fun main(args: Array<String>) = PlaypenCommand().main(args)

class PlaypenCommand : CliktCommand() {
    val name by argument()
    val numShapes by option("-n", "--num-shapes", help="Number of shapes to draw").int().default(10)
    val width by option("-w", "--width", help="Width of the playpen").int().default(800)
    val height by option("-h", "--height", help="Height of the playpen").int().default(800)

    override fun run() {
        JFrame(name).also {
            it.add(PlayPenPanel(numShapes, BoundsRectangle(0, 0, width, height)))

            it.pack()
            it.isVisible = true

            it.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        }
    }
}

class PlayPenPanel(val numShapes: Int, val bounds: BoundsRectangle) : JPanel() {
    init {
        preferredSize = bounds.toDimension()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g is Graphics2D) {
            repeat(numShapes) {
                drawRandomCubicCurve(g)
            }
        } else {
            throw Exception("Arghghghghgh!")
        }
    }

    private fun drawRandomCubicCurve(g: Graphics2D) {
        spawnRandomStrokedCubicCurveShape(bounds, evolverSettings).draw(g)
    }
}

private fun BoundsRectangle.toDimension() = Dimension(width, height)

val evolverSettings = EvolverSettings.default("name")
