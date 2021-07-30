package com.cdpjenkins.genetic.model

import kotlin.random.Random

internal fun mutateValueLinear(oldValue: Int, range: Int, min: Int, max: Int): Int {
    var newValue = oldValue + Random.nextInt(range * 2 + 1) - range
    if (newValue < min) newValue = min
    if (newValue > max) newValue = max
    return newValue
}

// This is the wrong place for this
internal fun randint(min: Int, max: Int): Int = Random.nextInt(max - min) + min

internal fun withProbability(probability: Double) = Random.nextDouble() < probability