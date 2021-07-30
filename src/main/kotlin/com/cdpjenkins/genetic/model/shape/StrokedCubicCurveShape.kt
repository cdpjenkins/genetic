package com.cdpjenkins.genetic.model.shape

import EvolverSettings
import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.geom.CubicCurve2D

class StrokedCubicCurveShape(
    val path: List<Point>,
    val colour: Colour,
    val bounds: BoundsRectangle
): Shape {
    override fun draw(g: Graphics2D) {
        val p1 = path[0]
        val p2 = path[1]
        val p3 = path[2]
        val p4 = path[3]

        val curve = CubicCurve2D.Float(
            p1.x.toFloat(), p1.y.toFloat(),
            p2.x.toFloat(), p2.y.toFloat(),
            p3.x.toFloat(), p3.y.toFloat(),
            p4.x.toFloat(), p4.y.toFloat())
        val TODO_width_shouldBeVariable = 4f
        val basicStroke = BasicStroke(TODO_width_shouldBeVariable, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.setStroke(basicStroke)
        g.setColor(colour.toAwtColor())
        g.draw(curve)
    }

    override fun mutate(evolverSettings: EvolverSettings): Shape {
        val newPath = path.map { it.mutate(bounds, evolverSettings) }
        val newColour = colour.mutate(evolverSettings)

        return StrokedCubicCurveShape(newPath, newColour, bounds)
    }
}
