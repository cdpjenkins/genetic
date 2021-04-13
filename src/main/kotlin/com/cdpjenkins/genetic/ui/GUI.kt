package com.cdpjenkins.genetic.ui

import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.makeIndividual
import java.awt.BorderLayout
import java.awt.LayoutManager
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.system.measureTimeMillis

class GUI(title: String? = "Genetic!") : JFrame(title) {
    constructor(): this("Genetic!") {
        contentPane.add(UIPanel())
        pack()
    }

    class UIPanel(layout: LayoutManager): JPanel(layout) {
        val masterImage: BufferedImage = ImageIO.read(File("cow.jpg"))
        val masterIcon: ImageIcon = ImageIcon(masterImage)

        constructor(): this(BorderLayout()) {
            val width = masterImage.width
            val height = masterImage.height

            val individual = makeIndividual(width, height, masterImage)

            add(JLabel(masterIcon), BorderLayout.WEST)
            add(JLabel(ImageIcon(individual.bufferedImage)), BorderLayout.EAST)

            val mutateButton = JButton("Mutate!")
            add(mutateButton, BorderLayout.SOUTH)
            mutateButton.addActionListener { e ->
                individual.mutate()
                repaint()
            }

            startTimer(individual)
        }

        private fun startTimer(individual: Individual) {
            val timer = Timer(20) { e ->
                val timeInMillis = measureTimeMillis {
                    individual.mutate()
                    repaint()
                }
            }
            timer.setInitialDelay(1000)
            timer.start()
        }
    }
}