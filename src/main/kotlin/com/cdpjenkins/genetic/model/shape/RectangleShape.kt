package com.cdpjenkins.genetic.model.shape

import java.awt.Graphics2D

data class RectangleShape(
    val topLeft: Point,
    val bottomRight: Point,
    val colour: Colour,
    val bounds: BoundsRectangle
): Shape {
    override fun draw(g: Graphics2D) {
        g.color = colour.toAwtColor()
        g.fillRect(topLeft.x, topLeft.y, bottomRight.x-topLeft.x, bottomRight.y-topLeft.y)
    }

    override fun mutate(): Shape {
        val newTopLeft = topLeft.mutate(bounds)
        val newBottomRight = bottomRight.mutate(bounds)
        val newColour = colour.mutate()
        return RectangleShape(newTopLeft, newBottomRight, newColour, bounds)
    }
}