package com.cdpjenkins.genetic.model

import java.awt.image.BufferedImage
import GENOME_SIZE
import MAX_ALPHA
import MIN_ALPHA

val polyVertices: Int = 4

fun makeIndividual(
    masterImage: BufferedImage,
    bounds: BoundsRectangle
): Individual {
    val shapes = (1..GENOME_SIZE).map { spawnRandomShape(
        bounds
    ) }
    return Individual(shapes, bounds)
}

fun spawnRandomShape(bounds: BoundsRectangle): Shape {
    val rand = randint(0, 4)
    return when (2) {
        0 -> spawnRandomCircle(bounds)
        1 -> spawnRandomRect(bounds)
        2 -> spawnRandomQuadCurveShape(bounds)
        3  -> spawnRandomTriangle(bounds)
        else -> throw AssertionError()
    }
}

fun spawnRandomRect(bounds: BoundsRectangle): RectangleShape {
    return RectangleShape(
        spawnRandomPoint(bounds),
        spawnRandomPoint(bounds),
        spawnRandomColour(),
        bounds
    )
}

fun spawnRandomCircle(bounds: BoundsRectangle) = Circle(
    centre = spawnRandomPoint(bounds),
    radius = spawnRandomRadius(),
    colour = spawnRandomColour(),
    bounds = bounds
)

private fun spawnRandomPoint(bounds: BoundsRectangle): Point {
    return Point(randint(bounds.minX, bounds.maxX), randint(bounds.minY, bounds.maxY))
}

fun spawnRandomQuadCurveShape(bounds: BoundsRectangle): QuadCurveShape {
    val centre = spawnRandomPoint(bounds)

    val vertices = (1..polyVertices).map { Point(randint(0, bounds.width), randint(0, bounds.height)) }
    val colour = spawnRandomColour()

    return QuadCurveShape(vertices, colour, bounds)
//    return QuadCurveShape(vertices, colour, BoundsRectangle(centre.x - 100, centre.y - 100, centre.x + 100, centre.y + 100))
}

fun spawnRandomTriangle(bounds: BoundsRectangle): PolygonShape {
    val centre = spawnRandomPoint(bounds)

    val vertices = (1..3).map { Point(randint(centre.x - 20, centre.x + 20), randint(centre.y - 20, centre.y + 20)) }
    val colour = spawnRandomColour()

    return PolygonShape(vertices, colour, bounds)
}

private fun spawnRandomRadius() = random.nextInt(25)

private fun spawnRandomColour() =
    Colour(
        red = random.nextInt(255),
        green = random.nextInt(255),
        blue = random.nextInt(255),
        alpha = randint(MIN_ALPHA, MAX_ALPHA)
    )
