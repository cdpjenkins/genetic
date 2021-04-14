package com.cdpjenkins.genetic.model

import java.awt.Graphics2D

interface Shape {
    fun draw(g: Graphics2D)
    fun mutate(): Shape
    fun maybeMutate(): Shape = if (randint(0, 100) < 5) mutate() else this
}