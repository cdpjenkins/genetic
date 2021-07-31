package com.cdpjenkins.genetic.model.shape

import EvolverSettings
import com.cdpjenkins.genetic.model.mutateValueGaussian

data class Point(val x: Int, val y: Int) {
    fun mutate(bounds: BoundsRectangle, evolverSettings: EvolverSettings): Point {
        val newX = mutateValueGaussian(x, evolverSettings.pointMutateRange, bounds.minX, bounds.maxX)
        val newY = mutateValueGaussian(y, evolverSettings.pointMutateRange, bounds.minY, bounds.maxY)
        val newCentre = Point(newX, newY)
        return newCentre
    }

    override fun toString(): String {
        return "Point($x, $y)"
    }
}
