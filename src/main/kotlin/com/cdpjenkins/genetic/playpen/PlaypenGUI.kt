package com.cdpjenkins.genetic.playpen

import java.awt.*
import java.awt.geom.CubicCurve2D
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.reflect.cast

fun main(args: Array<String>) {
    JFrame("Playpen").also {

        it.add(PlayPenPanel().also {
        })

        it.pack()
        it.isVisible = true
    }
}

class PlayPenPanel: JPanel() {
    init {
        preferredSize = Dimension(200, 200)
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g is Graphics2D) {
            val curve = CubicCurve2D.Double(10.0, 10.0, 100.0, 10.0, 10.0, 100.0, 100.0, 100.0)
            val basicStroke = BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.setStroke(basicStroke)
            g.draw(curve)
        } else {
            throw Exception("Arghghghghgh!")
        }
    }
}
