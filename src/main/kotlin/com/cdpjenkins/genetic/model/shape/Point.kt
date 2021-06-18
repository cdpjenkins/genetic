package com.cdpjenkins.genetic.model.shape

import EvolverSettings
import com.cdpjenkins.genetic.model.mutateValueLinear

data class Point(val x: Int, val y: Int) {
    fun mutate(bounds: BoundsRectangle, evolverSettings: EvolverSettings): Point {
        val newX = mutateValueLinear(x, evolverSettings.pointMutateRange, bounds.minX, bounds.maxX)
        val newY = mutateValueLinear(y, evolverSettings.pointMutateRange, bounds.minY, bounds.maxY)
        val newCentre = Point(newX, newY)
        return newCentre
    }
}
