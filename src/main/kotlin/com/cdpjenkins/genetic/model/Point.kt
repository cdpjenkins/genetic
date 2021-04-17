package com.cdpjenkins.genetic.model

import POINT_MUTATE_RANGE

data class Point(val x: Int, val y: Int) {
    fun mutate(bounds: BoundsRectangle): Point {
        val newX = mutateValueLinear(x, POINT_MUTATE_RANGE, bounds.minX, bounds.maxX)
        val newY = mutateValueLinear(y, POINT_MUTATE_RANGE, bounds.minY, bounds.maxY)
        val newCentre = Point(newX, newY)
        return newCentre
    }
}
