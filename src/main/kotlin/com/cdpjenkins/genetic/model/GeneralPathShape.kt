package com.cdpjenkins.genetic.model

import java.awt.Graphics2D
import java.awt.geom.GeneralPath

abstract class GeneralPathShape(
    val path: List<Point>,
    val colour: Colour
) : Shape {
    protected val generalPath: GeneralPath by lazy(this::makeGeneralPath)

    abstract fun makeGeneralPath(): GeneralPath

    override fun draw(g: Graphics2D) {
        g.color = colour.getColor()
        g.fill(generalPath)
    }
}