package com.cdpjenkins.genetic.model

import java.util.*

internal fun mutateValueLinear(oldValue: Int, range: Int, min: Int, max: Int): Int {
    var newValue = oldValue + random.nextInt(range * 2) - range
    if (newValue < min) newValue = min
    if (newValue > max) newValue = max
    return newValue
}


// This is the wrong place for this
// Also TODO apparently there is a Kotlin Random class
internal val random = Random()
internal fun randint(min: Int, max: Int): Int = random.nextInt(max - min) + min
