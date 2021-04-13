package com.cdpjenkins.genetic.model

import COLOUR_MUTATE_AMOUNT
import MAX_ALPHA
import MIN_ALPHA
import java.awt.Color

data class Colour(val r: Int, val g:Int, val b: Int, val a:Int) {
    fun getColor(): Color {
        try {
            return Color(r, g, b, a)
        } catch (e: Exception) {
            println("$this")
            throw e
        }
    }
    fun mutate(): Colour {
        val red = mutateValueLinear(r, COLOUR_MUTATE_AMOUNT, 0, 255)
        val green = mutateValueLinear(g, COLOUR_MUTATE_AMOUNT, 0, 255)
        val blue = mutateValueLinear(b, COLOUR_MUTATE_AMOUNT, 0, 255)
        val alpha = mutateValueLinear(a, COLOUR_MUTATE_AMOUNT, MIN_ALPHA, MAX_ALPHA)

        val newColour = Colour(red, green, blue, alpha)
        return newColour
    }
}