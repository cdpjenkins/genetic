package com.cdpjenkins.genetic.model.shape

import POINT_MUTATE_RANGE
import com.cdpjenkins.genetic.model.mutateValueLinear

data class Point(val x: Int, val y: Int) {
    fun mutate(bounds: BoundsRectangle): Point {
        val newX = mutateValueLinear(x, POINT_MUTATE_RANGE, bounds.minX, bounds.maxX)
        val newY = mutateValueLinear(y, POINT_MUTATE_RANGE, bounds.minY, bounds.maxY)
        val newCentre = Point(newX, newY)
        return newCentre
    }
}
