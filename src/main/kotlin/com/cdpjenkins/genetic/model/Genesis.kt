package com.cdpjenkins.genetic.model

import java.awt.image.BufferedImage
import GENOME_SIZE
import MAX_ALPHA
import MIN_ALPHA

val polyVertices: Int = 4

fun makeIndividual(
    width: Int,
    height: Int,
    masterImage: BufferedImage
): Individual {
    val shapes = (1..GENOME_SIZE).map { spawnRandomShape(width, height) }
    return Individual(shapes, masterImage, width, height)
}

fun spawnRandomShape(width: Int, height: Int): Shape {
    val rand = randint(0, 4)
    return when (3) {
        0 -> spawnRandomCircle(width, height)
        1 -> spawnRandomRect(width, height)
        2 -> spawnRandomGeneralPathShape(width, height)
        3  -> spawnRandomTriangle(width, height)
        else -> throw AssertionError()
    }
}

fun spawnRandomRect(width: Int, height: Int): RectangleShape {
    return RectangleShape(
        spawnRandomPoint(width, height),
        spawnRandomPoint(width, height),
        spawnRandomColour(),
        width,
        height
    )
}

fun spawnRandomCircle(width: Int, height: Int): Circle = Circle(
    centre = spawnRandomPoint(width, height),
    radius = spawnRandomRadius(),
    colour = spawnRandomColour(),
    width = width,
    height = height
)

private fun spawnRandomPoint(
    width: Int,
    height: Int
) = Point(random.nextInt(width), random.nextInt(height))

fun spawnRandomGeneralPathShape(width: Int, height: Int): QuadCurveShape {
    val centre = spawnRandomPoint(width, height)

    val vertices = (0..polyVertices).map { Point(randint(centre.x - 20, centre.x + 20), randint(centre.y - 20, centre.y + 20)) }
    val colour = spawnRandomColour()

    return QuadCurveShape(vertices, colour, width, height)
}

fun spawnRandomTriangle(width: Int, height: Int): PolygonShape {
    val centre = spawnRandomPoint(width, height)

    val vertices = (1..3).map { Point(randint(centre.x - 20, centre.x + 20), randint(centre.y - 20, centre.y + 20)) }
    val colour = spawnRandomColour()

    return PolygonShape(vertices, colour, width, height)
}

private fun spawnRandomRadius() = random.nextInt(25)

private fun spawnRandomColour() =
    Colour(
        red = random.nextInt(255),
        green = random.nextInt(255),
        blue = random.nextInt(255),
        alpha = randint(MIN_ALPHA, MAX_ALPHA)
    )
