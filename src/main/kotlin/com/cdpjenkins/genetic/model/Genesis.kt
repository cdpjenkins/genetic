package com.cdpjenkins.genetic.model

import java.awt.image.BufferedImage
import GENOME_SIZE
import MAX_ALPHA
import MIN_ALPHA
import random

fun makeIndividual(
    width: Int,
    height: Int,
    masterImage: BufferedImage
): Individual {
    val shapes = (1..GENOME_SIZE).map { spawnRandomShape(width, height) }
    return Individual(shapes, masterImage, width, height)
}

fun spawnRandomShape(width: Int, height: Int): Circle {
    return Circle(
        random.nextInt(width),
        random.nextInt(height),
        random.nextInt(50),
        Colour(random.nextInt(255), random.nextInt(255), random.nextInt(255), randint(MIN_ALPHA, MAX_ALPHA)),
        width,
        height
    )
}
