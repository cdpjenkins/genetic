package com.cdpjenkins.genetic.model

import java.awt.Graphics2D
import java.awt.geom.GeneralPath

class GeneralPathShape(val path: List<Point>, val colour: Colour, val width: Int, val height: Int): Shape {

    private val generalPath: GeneralPath by lazy(this::makeGeneralPath)

    override fun draw(g: Graphics2D) {
        g.color = colour.getColor()
        g.fill(generalPath)
        makeGeneralPath()
    }

    override fun mutate(): Shape {
        val newPath = path.map { it.mutate(width, height) }
        val newColour = colour.mutate()

        return GeneralPathShape(newPath, newColour, width, height)
    }

    private fun makeGeneralPath(): GeneralPath {
        val generalPath = GeneralPath(GeneralPath.WIND_EVEN_ODD, path.size)
        generalPath.moveTo(path[path.size - 1].x.toDouble(), path[path.size - 1].y.toDouble())

        for (pair in path.zipWithNext()) {
            val p1 = pair.first
            val p2 = pair.second
            generalPath.quadTo(p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble())
        }

        return generalPath
    }
}

