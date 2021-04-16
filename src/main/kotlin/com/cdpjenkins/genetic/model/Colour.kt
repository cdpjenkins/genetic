package com.cdpjenkins.genetic.model

import COLOUR_MUTATE_AMOUNT
import MAX_ALPHA
import MIN_ALPHA
import java.awt.Color

data class Colour(val red: Int, val green:Int, val blue: Int, val alpha:Int) {
    fun toAwtColor(): Color {
        try {
            return Color(red, green, blue, alpha)
        } catch (e: Exception) {
            println("$this")
            throw e
        }
    }

    fun mutate(): Colour {
        val red = mutateValueLinear(red, COLOUR_MUTATE_AMOUNT, 0, 255)
        val green = mutateValueLinear(green, COLOUR_MUTATE_AMOUNT, 0, 255)
        val blue = mutateValueLinear(blue, COLOUR_MUTATE_AMOUNT, 0, 255)
        val alpha = mutateValueLinear(alpha, COLOUR_MUTATE_AMOUNT, MIN_ALPHA, MAX_ALPHA)

        val newColour = Colour(red, green, blue, alpha)
        return newColour
    }
}