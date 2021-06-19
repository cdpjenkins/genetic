package com.cdpjenkins.genetic.model

import EvolverSettings
import com.cdpjenkins.genetic.model.shape.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.random.Random

val logger: Logger = LoggerFactory.getLogger(object{}::class.java)
val polyVertices: Int = 4

fun makeIndividual(
    bounds: BoundsRectangle,
    evolverSettings: EvolverSettings
): Individual {
    val shapes = (1..evolverSettings.initialGenomeSize).map {
        spawnRandomShape(bounds, evolverSettings)
    }
    return Individual(shapes, bounds)
}

fun spawnRandomShape(bounds: BoundsRectangle, evolverSettings: EvolverSettings): Shape {
    logger.info("Try spawning a random shape")

    val rand = randint(0, 4)
    return when (2) {
        0 -> spawnRandomCircle(bounds, evolverSettings)
        1 -> spawnRandomRect(bounds, evolverSettings)
        2 -> spawnRandomQuadCurveShape(bounds, evolverSettings)
        3  -> spawnRandomTriangle(bounds, evolverSettings)
        else -> throw AssertionError()
    }
}

fun spawnRandomRect(bounds: BoundsRectangle, evolverSettings: EvolverSettings): RectangleShape {
    return RectangleShape(
        spawnRandomPoint(bounds),
        spawnRandomPoint(bounds),
        spawnRandomColour(evolverSettings),
        bounds
    )
}

fun spawnRandomCircle(bounds: BoundsRectangle, evolverSettings: EvolverSettings) = Circle(
    centre = spawnRandomPoint(bounds),
    radius = spawnRandomRadius(),
    colour = spawnRandomColour(evolverSettings),
    bounds = bounds
)

private fun spawnRandomPoint(bounds: BoundsRectangle): Point {
    return Point(randint(bounds.minX, bounds.maxX), randint(bounds.minY, bounds.maxY))
}

fun spawnRandomQuadCurveShape(bounds: BoundsRectangle, evolverSettings: EvolverSettings): QuadCurveShape {
    val centre = spawnRandomPoint(bounds)
    val shapeBounds = BoundsRectangle(centre.x - 100, centre.y - 100, centre.x + 100, centre.y + 100)
//    val shapeBounds = bounds
    val vertices = (1..polyVertices).map {
        spawnRandomPoint(
            BoundsRectangle(centre.x-10, centre.y-10, centre.x+10, centre.y+10)
        )
    }
    val colour = spawnRandomColour(evolverSettings)

    return QuadCurveShape(vertices, colour, shapeBounds)
}

fun spawnRandomTriangle(bounds: BoundsRectangle, evolverSettings: EvolverSettings): PolygonShape {
    val centre = spawnRandomPoint(bounds)

    val vertices = (1..3).map { Point(randint(centre.x - 20, centre.x + 20), randint(centre.y - 20, centre.y + 20)) }
    val colour = spawnRandomColour(evolverSettings)

    return PolygonShape(vertices, colour, bounds)
}

private fun spawnRandomRadius() = Random.nextInt(25)

private fun spawnRandomColour(evolverSettings: EvolverSettings) =
    Colour(
        red = Random.nextInt(255),
        green = Random.nextInt(255),
        blue = Random.nextInt(255),
        alpha = randint(evolverSettings.minAlpha, evolverSettings.maxAlpha)
    )
