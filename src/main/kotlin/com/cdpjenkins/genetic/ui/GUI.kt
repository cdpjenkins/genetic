package com.cdpjenkins.genetic.ui

import com.cdpjenkins.genetic.model.Evolver
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.saveToDisk
import java.awt.BorderLayout
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.*

class GUI(
    masterImage: BufferedImage,
    evolver: Evolver
) : JFrame("Genetic!") {
    init {
        contentPane.add(EvolverPanel(masterImage, evolver))
        pack()
    }

    class EvolverPanel(
        masterImage: BufferedImage,
        val evolver: Evolver
    ): JPanel(BorderLayout()) {
        val masterIcon: ImageIcon = ImageIcon(masterImage)

        init {
            add(JLabel(masterIcon), BorderLayout.WEST)
            val individualImageLabel = JLabel(ImageIcon(masterImage))
            add(individualImageLabel, BorderLayout.EAST)

            val fitnessLabel = JLabel("", SwingConstants.RIGHT)
            add(fitnessLabel, BorderLayout.SOUTH)

            val swingWorker = EvolverWorker(this.evolver)
            swingWorker.addListener {
                updateUi(individualImageLabel, it, fitnessLabel)
            }

            swingWorker.addListener {
                it.saveToDisk()
            }
            swingWorker.execute()
        }

        private fun updateUi(
            individualImageLabel: JLabel,
            it: Individual,
            fitnessLabel: JLabel
        ) {
            individualImageLabel.icon = ImageIcon(it.bufferedImage)
            fitnessLabel.text = it.describe()
            individualImageLabel.invalidate()
            repaint()
        }
    }
}

fun ensureDirExists(dirName: String) {
    val dir = File(dirName)
    if (!dir.exists()) dir.mkdir()
}
