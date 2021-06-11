package com.cdpjenkins.genetic.model

import GENOME_SIZE
import MAX_ALPHA
import MIN_ALPHA
import com.cdpjenkins.genetic.model.shape.*
import java.awt.image.BufferedImage
import kotlin.random.Random

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

    println("Spawning a random shape!!!1")

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
    val shapeBounds = BoundsRectangle(centre.x - 100, centre.y - 100, centre.x + 100, centre.y + 100)
//    val shapeBounds = bounds
    val vertices = (1..polyVertices).map {
        spawnRandomPoint(
            BoundsRectangle(centre.x-10, centre.y-10, centre.x+10, centre.y+10)
        )
    }
    val colour = spawnRandomColour()

    return QuadCurveShape(vertices, colour, shapeBounds)
}

fun spawnRandomTriangle(bounds: BoundsRectangle): PolygonShape {
    val centre = spawnRandomPoint(bounds)

    val vertices = (1..3).map { Point(randint(centre.x - 20, centre.x + 20), randint(centre.y - 20, centre.y + 20)) }
    val colour = spawnRandomColour()

    return PolygonShape(vertices, colour, bounds)
}

private fun spawnRandomRadius() = Random.nextInt(25)

private fun spawnRandomColour() =
    Colour(
        red = Random.nextInt(255),
        green = Random.nextInt(255),
        blue = Random.nextInt(255),
        alpha = randint(MIN_ALPHA, MAX_ALPHA)
    )
