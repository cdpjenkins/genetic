package com.cdpjenkins.genetic.model

import java.awt.geom.GeneralPath

class QuadCurveShape(
    path: List<Point>,
    colour: Colour,
    width: Int,
    height: Int
): GeneralPathShape(
    path,
    colour,
    width,
    height
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
        val newPath = path.map { it.mutate(width, height) }
        val newColour = colour.mutate()

        return QuadCurveShape(newPath, newColour, width, height)
    }
}
