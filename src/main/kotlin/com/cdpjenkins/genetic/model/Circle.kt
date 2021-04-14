package com.cdpjenkins.genetic.model

import java.awt.Graphics2D

class Circle(
    val centre: Point,
    val radius: Radius,
    val colour: Colour,
    val width: Int,
    val height: Int
) : Shape {
    override fun draw(g: Graphics2D) {
        g.color = colour.getColor()
        g.fillOval(centre.x - radius, centre.y - radius, radius*2, radius*2)
    }

    override fun mutate(): Circle {
        return Circle(
            centre.mutate(width, height),
            radius.mutate(),
            colour.mutate(),
            width,
            height
        )
    }
}

typealias Radius = Int
fun Radius.mutate() = mutateValueLinear(this, 5, 1, 20)

