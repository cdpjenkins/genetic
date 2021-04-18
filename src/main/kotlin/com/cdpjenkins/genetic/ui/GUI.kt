package com.cdpjenkins.genetic.ui

import MASTER_IMAGE_FILE
import com.cdpjenkins.genetic.image.writePng
import com.cdpjenkins.genetic.model.BoundsRectangle
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.svg.SvgRenderer
import json.JSON
import java.awt.BorderLayout
import java.awt.LayoutManager
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

class GUI(initialIndividual: Individual? = null) : JFrame("Genetic!") {
    init {
        contentPane.add(UIPanel(initialIndividual))
        pack()
    }

    class UIPanel(layout: LayoutManager): JPanel(layout) {
        val masterImage: BufferedImage = ImageIO.read(File(MASTER_IMAGE_FILE))
        val masterIcon: ImageIcon = ImageIcon(masterImage)

        constructor(initialIndividual: Individual?) : this(BorderLayout()) {
            val bounds = BoundsRectangle(0, 0, masterImage.width, masterImage.height)

            add(JLabel(masterIcon), BorderLayout.WEST)
            val individualImageLabel = JLabel(ImageIcon(masterImage))
            add(individualImageLabel, BorderLayout.EAST)

            val fitnessLabel = JLabel("", SwingConstants.RIGHT)
            add(fitnessLabel, BorderLayout.SOUTH)

            val evolver = makeEvolver(bounds, masterImage, initialIndividual)
            val swingWorker = EvolverWorker(evolver)
            swingWorker.addListener {
                individualImageLabel.icon = ImageIcon(it.bufferedImage)
                val size = it.genome.size
                val time = it.timeInMillis
                val generation = it.generation
                val fitness = it.fitness
                fitnessLabel.text =
                    "Genome size: $size Time: $time Generation: $generation Fitness: $fitness"
                individualImageLabel.invalidate()
                repaint()
            }

            ensureDirExists("output")
            ensureDirExists("output/png")
            ensureDirExists("output/json")
            ensureDirExists("output/svg")
            swingWorker.addListener {
                if (it.generation % 10 == 0) {
                    val outputFile =
                        File(String.format("output/png/cow_%010d.png", it.generation))
                    writePng(it, outputFile)

                    JSON().serialiseToFile(File(String.format("output/json/cow_%010d.json", it.generation)), it)

                    SvgRenderer().render(File(String.format("output/svg/cow_%010d.svg", it.generation)), it)
                }
            }
            swingWorker.execute()
        }

        private fun ensureDirExists(dirName: String) {
            val dir = File(dirName)
            if (!dir.exists()) dir.mkdir()
        }
    }
}
