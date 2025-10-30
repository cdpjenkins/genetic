package com.cdpjenkins.genetic.model.shape

import EvolverSettings
import com.cdpjenkins.genetic.evolver.mutateValueGaussian
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Color

val logger = KotlinLogging.logger {}

data class Colour(val red: Int, val green:Int, val blue: Int, val alpha:Int) {
    fun toAwtColor(): Color {
        try {
            return Color(red, green, blue, alpha)
        } catch (e: Exception) {
            logger.error(e) { "Failed to convert colour to AWT Color" }
            throw e
        }
    }

    fun mutate(evolverSettings: EvolverSettings): Colour {
        val red = mutateValueGaussian(red, evolverSettings.colourMutateAmount, 0, 255)
        val green = mutateValueGaussian(green, evolverSettings.colourMutateAmount, 0, 255)
        val blue = mutateValueGaussian(blue, evolverSettings.colourMutateAmount, 0, 255)
        val alpha = mutateValueGaussian(alpha, evolverSettings.colourMutateAmount, evolverSettings.minAlpha, evolverSettings.maxAlpha)

        val newColour = Colour(red, green, blue, alpha)
        return newColour
    }

    override fun toString(): String {
        return "Colour($red, $green, $blue, $alpha)"
    }
}
