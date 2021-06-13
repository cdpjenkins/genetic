package com.cdpjenkins.genetic.model.shape

import EvolverSettings
import com.cdpjenkins.genetic.model.mutateValueLinear
import java.awt.Graphics2D

data class Circle(
    val centre: Point,
    val radius: Radius,
    val colour: Colour,
    val bounds: BoundsRectangle
) : Shape {
    override fun draw(g: Graphics2D) {
        g.color = colour.toAwtColor()
        g.fillOval(centre.x - radius, centre.y - radius, radius*2, radius*2)
    }

    override fun mutate(evolverSettings: EvolverSettings): Circle {
        return Circle(
            centre.mutate(bounds),
            radius.mutate(),
            colour.mutate(evolverSettings),
            bounds
        )
    }
}

typealias Radius = Int
fun Radius.mutate() = mutateValueLinear(this, 5, 1, 20)

