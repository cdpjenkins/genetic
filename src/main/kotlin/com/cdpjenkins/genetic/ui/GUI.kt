package com.cdpjenkins.genetic.ui

import com.cdpjenkins.genetic.model.*
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


            add(JLabel(masterIcon), BorderLayout.WEST)
            val individualImageLabel = JLabel(ImageIcon(masterImage))
            add(individualImageLabel, BorderLayout.EAST)

            val fitnessLabel = JLabel("", SwingConstants.RIGHT)
            add(fitnessLabel, BorderLayout.SOUTH)

            val evolver = makeEvolver(width, height, masterImage)
            evolver.addListener {
                individualImageLabel.icon = ImageIcon(it.bufferedImage)
                fitnessLabel.text = "Generation: ${it.generation} Fitness: ${it.fitness}"
                individualImageLabel.invalidate()
                repaint()
            }
            evolver.addListener {
                if (it.generation % 10 == 0) {
                    val outputFile =
                        File(String.format("output/cow_%010d.png", it.generation))
                    ImageIO.write(it.bufferedImage, "png", outputFile)
                }
            }

            startTimer(evolver)
        }

        private fun startTimer(evolver: Evolver) {
            val timer = Timer(20) { e ->
                val timeInMillis = measureTimeMillis {
                    evolver.mutate()
                    repaint()
                }
//                println("time taken: $timeInMillis")
            }
            timer.setInitialDelay(1000)
            timer.start()
        }
    }
}