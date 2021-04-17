package com.cdpjenkins.genetic.image

import com.cdpjenkins.genetic.model.Individual
import java.io.File
import javax.imageio.ImageIO

fun writePng(individual: Individual, outputFile: File) {
    ImageIO.write(individual.bufferedImage, "png", outputFile)
}
