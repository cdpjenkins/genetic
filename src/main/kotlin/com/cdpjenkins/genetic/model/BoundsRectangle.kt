package com.cdpjenkins.genetic.model

data class BoundsRectangle(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
    val width: Int = maxX - minX
    val height: Int = maxY - minY
}
