package com.cdpjenkins.genetic.model

import random

internal fun mutateValueLinear(oldValue: Int, range: Int, min: Int, max: Int): Int {
    if (randint(0, 10000) < 5) {
        var newValue = oldValue + random.nextInt(range * 2) - range
        if (newValue < min) newValue = min
        if (newValue > max) newValue = max
        return newValue
    } else {
        return oldValue
    }
}

internal fun randint(min: Int, max: Int): Int = random.nextInt(max - min) + min
