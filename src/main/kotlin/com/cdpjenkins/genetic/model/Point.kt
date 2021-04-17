package com.cdpjenkins.genetic.model

data class Point(val x: Int, val y: Int) {
    fun mutate(bounds: BoundsRectangle): Point {
        val newX = mutateValueLinear(x, 5, bounds.minX, bounds.maxX)
        val newY = mutateValueLinear(y, 5, bounds.minY, bounds.maxY)
        val newCentre = Point(newX, newY)
        return newCentre
    }
}
