package com.cdpjenkins.genetic.model

import java.awt.Graphics2D

data class Circle(
    val x: Int,
    val y: Int,
    private val radius: Int,
    val colour: Colour,
    val width: Int,
    val height: Int
) : Shape {

    override fun draw(g: Graphics2D) {
        g.color = colour.getColor()
        g.fillOval(x - radius/2, y - radius/2, radius, radius)
    }

    override fun mutate(): Circle {
        val newX = mutateValueLinear(x, 5, 0, width)
        val newY = mutateValueLinear(y, 5, 0, height)
        val newRadius = mutateValueLinear(radius, 5, 1, 20)

        return Circle(newX, newY, newRadius, colour.mutate(), width, height)
    }
}