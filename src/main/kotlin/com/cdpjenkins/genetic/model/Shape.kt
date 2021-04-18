package com.cdpjenkins.genetic.model

import SHAPE_MUTATE_CHANCE
import java.awt.Graphics2D

interface Shape {
    fun draw(g: Graphics2D)
    fun mutate(): Shape
    fun maybeMutate(): Shape = if (randint(0, SHAPE_MUTATE_CHANCE) == 0) mutate() else this
}
