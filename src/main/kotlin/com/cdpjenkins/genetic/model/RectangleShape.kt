package com.cdpjenkins.genetic.model

import java.awt.Graphics2D

class RectangleShape(
    val topLeft: Point,
    val bottomRight: Point,
    val colour: Colour,
    val width: Int,
    val height: Int
): Shape {
    override fun draw(g: Graphics2D) {
        g.color = colour.getColor()
        g.fillRect(topLeft.x, topLeft.y, bottomRight.x-topLeft.x, bottomRight.y-topLeft.y)
    }

    override fun mutate(): Shape {
        val newTopLeft = topLeft.mutate(width, height)
        val newBottomRight = bottomRight.mutate(width, height)
        val newColour = colour.mutate()
        return RectangleShape(newTopLeft, newBottomRight, newColour, width, height)
    }
}