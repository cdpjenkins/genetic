package com.cdpjenkins.genetic.model

import java.awt.geom.GeneralPath

class QuadCurveShape(
    path: List<Point>,
    colour: Colour,
    val bounds: BoundsRectangle
): GeneralPathShape(
    path,
    colour
) {
    override fun makeGeneralPath(): GeneralPath {
        val generalPath = GeneralPath(GeneralPath.WIND_EVEN_ODD, path.size)
        generalPath.moveTo(path[path.size - 1].x.toDouble(), path[path.size - 1].y.toDouble())

        for (pair in path.zipWithNext()) {
            val p1 = pair.first
            val p2 = pair.second
            generalPath.quadTo(p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble())
        }

        return generalPath
    }

    override fun mutate(): Shape {
        val newPath = path.map { it.mutate(bounds) }
        val newColour = colour.mutate()

        return QuadCurveShape(newPath, newColour, bounds)
    }
}
