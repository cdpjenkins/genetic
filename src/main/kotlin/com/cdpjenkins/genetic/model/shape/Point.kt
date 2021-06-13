package com.cdpjenkins.genetic.model.shape

import com.cdpjenkins.genetic.model.mutateValueLinear
import evolverSettings

data class Point(val x: Int, val y: Int) {
    fun mutate(bounds: BoundsRectangle): Point {
        val newX = mutateValueLinear(x, evolverSettings.pointMutateRange, bounds.minX, bounds.maxX)
        val newY = mutateValueLinear(y, evolverSettings.pointMutateRange, bounds.minY, bounds.maxY)
        val newCentre = Point(newX, newY)
        return newCentre
    }
}
