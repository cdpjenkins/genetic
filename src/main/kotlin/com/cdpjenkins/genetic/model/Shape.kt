package com.cdpjenkins.genetic.model

import java.awt.Graphics2D

interface Shape {
    fun draw(g: Graphics2D)
    fun mutate(): Circle
}