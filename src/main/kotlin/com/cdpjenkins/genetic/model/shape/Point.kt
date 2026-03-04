package com.cdpjenkins.genetic.model.shape

import com.cdpjenkins.genetic.evolver.EvolverSettings
import com.cdpjenkins.genetic.evolver.mutateValueGaussian
import com.cdpjenkins.genetic.evolver.mutateValueGaussianUnbounded

data class Point(val x: Int, val y: Int) {
    fun mutate(bounds: BoundsRectangle, evolverSettings: EvolverSettings): Point {
        val newX = mutateValueGaussian(x, evolverSettings.pointMutateRange, bounds.minX, bounds.maxX)
        val newY = mutateValueGaussian(y, evolverSettings.pointMutateRange, bounds.minY, bounds.maxY)
        val newCentre = Point(newX, newY)
        return newCentre
    }

    fun mutateUnbounded(evolverSettings: EvolverSettings): Point {
        val newX = mutateValueGaussianUnbounded(x, evolverSettings.pointMutateRange)
        val newY = mutateValueGaussianUnbounded(y, evolverSettings.pointMutateRange)
        return Point(newX, newY)
    }

    override fun toString(): String {
        return "Point($x, $y)"
    }
}
