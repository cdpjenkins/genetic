package com.cdpjenkins.genetic.model.shape

import java.awt.geom.GeneralPath

data class QuadCurveShape(
    val quadCurvePath: List<Point>,
    val quadCurveColour: Colour,
    val bounds: BoundsRectangle
): GeneralPathShape(
    quadCurvePath,
    quadCurveColour
) {
    override fun makeGeneralPath(): GeneralPath {
        val generalPath = GeneralPath(GeneralPath.WIND_EVEN_ODD, quadCurvePath.size)
        generalPath.moveTo(quadCurvePath[quadCurvePath.size - 1].x.toDouble(), quadCurvePath[quadCurvePath.size - 1].y.toDouble())

        for (chunk in quadCurvePath.chunked(2)) {
            val p1 = chunk[0]
            val p2 = chunk[1]
            generalPath.quadTo(p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble())
        }

        return generalPath
    }

    override fun mutate(): Shape {
        val newPath = quadCurvePath.map { it.mutate(bounds) }
        val newColour = quadCurveColour.mutate()

        return QuadCurveShape(newPath, newColour, bounds)
    }
}
