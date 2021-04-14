package com.cdpjenkins.genetic.model

class Point(val x: Int, val y: Int) {
    fun mutate(width: Int, height: Int): Point {
        val newX = mutateValueLinear(x, 5, 0, width)
        val newY = mutateValueLinear(y, 5, 0, height)
        val newCentre = Point(newX, newY)
        return newCentre
    }
}
