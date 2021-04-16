package com.cdpjenkins.genetic.ui

import com.cdpjenkins.genetic.model.BoundsRectangle
import com.cdpjenkins.genetic.model.makeEvolver
import MASTER_IMAGE_FILE
import java.awt.BorderLayout
import java.awt.LayoutManager
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

class GUI(title: String? = "Genetic!") : JFrame(title) {
    constructor(): this("Genetic!") {
        contentPane.add(UIPanel())
        pack()
    }

    class UIPanel(layout: LayoutManager): JPanel(layout) {
        val masterImage: BufferedImage = ImageIO.read(File(MASTER_IMAGE_FILE))
        val masterIcon: ImageIcon = ImageIcon(masterImage)

        constructor() : this(BorderLayout()) {
            val bounds = BoundsRectangle(0, 0, masterImage.width, masterImage.height)

            add(JLabel(masterIcon), BorderLayout.WEST)
            val individualImageLabel = JLabel(ImageIcon(masterImage))
            add(individualImageLabel, BorderLayout.EAST)

            val fitnessLabel = JLabel("", SwingConstants.RIGHT)
            add(fitnessLabel, BorderLayout.SOUTH)

            val evolver = makeEvolver(masterImage, bounds)
            val swingWorker = EvolverWorker(evolver)
            swingWorker.addListener {
                individualImageLabel.icon = ImageIcon(it.bufferedImage)
                fitnessLabel.text = "Time: ${it.timeInMillis} Generation: ${it.generation} Fitness: ${it.fitness}"
                individualImageLabel.invalidate()
                repaint()
            }
            swingWorker.addListener {
                if (it.generation % 100 == 0) {
                    val outputFile =
                        File(String.format("output/cow_%010d.png", it.generation))
                    ImageIO.write(it.bufferedImage, "png", outputFile)
                }
            }
            swingWorker.execute()
        }
    }
}
