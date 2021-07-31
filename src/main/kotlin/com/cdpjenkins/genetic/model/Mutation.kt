package com.cdpjenkins.genetic.model

import kotlin.random.Random
import kotlin.random.asJavaRandom

internal fun mutateValueLinear(oldValue: Int, range: Int, min: Int, max: Int): Int {
    return (oldValue + Random.nextInt(range * 2 + 1) - range)
        .constrain(min, max)
}

internal fun mutateValueGaussian(oldValue: Int, stdDev: Int, min: Int, max: Int): Int {
    return (oldValue + Random.asJavaRandom().nextGaussian() * stdDev).toInt()
        .constrain(min, max)
}

private fun Int.constrain(min: Int, max: Int) =
    if (this < min) min
    else if (this > max) max
    else this

// This is the wrong place for this
internal fun randint(min: Int, max: Int): Int = Random.nextInt(max - min) + min

internal fun withProbability(probability: Double) = Random.nextDouble() < probability