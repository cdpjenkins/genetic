package com.cdpjenkins.genetic.model

import java.awt.geom.GeneralPath

class PolygonShape(
    path: List<Point>,
    colour: Colour,
    val bounds: BoundsRectangle
) : GeneralPathShape(path, colour) {
    override fun makeGeneralPath(): GeneralPath {
        val generalPath = GeneralPath(GeneralPath.WIND_EVEN_ODD, path.size)
        generalPath.moveTo(path[path.size - 1].x.toDouble(), path[path.size - 1].y.toDouble())

        for (point in path) {
            generalPath.lineTo(point.x.toDouble(), point.y.toDouble())
        }

        return generalPath
    }

    override fun mutate(): Shape {
        val newPath = path.map { it.mutate(bounds) }
        val newColour = colour.mutate()

        return PolygonShape(newPath, newColour, bounds)
    }
}
