package com.cdpjenkins.genetic.image

import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber

fun grabPixels(image: BufferedImage): IntArray {
    val pixels = IntArray(image.width * image.height)
    val pixelGrabber = PixelGrabber(image, 0, 0, image.width, image.height, pixels, 0, image.width)
    pixelGrabber.grabPixels()
    return pixels
}