package com.cdpjenkins.genetic.ui

import com.cdpjenkins.genetic.model.Evolver
import com.cdpjenkins.genetic.model.Individual
import java.awt.BorderLayout
import java.awt.image.BufferedImage
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
        evolver: Evolver
    ): JPanel(BorderLayout()) {
        val masterIcon: ImageIcon = ImageIcon(masterImage)

        init {
            add(JLabel(masterIcon), BorderLayout.WEST)
            val individualImageLabel = JLabel(ImageIcon(masterImage))
            add(individualImageLabel, BorderLayout.EAST)

            val fitnessLabel = JLabel("", SwingConstants.RIGHT)
            add(fitnessLabel, BorderLayout.SOUTH)

            evolver.addListener {
                SwingUtilities.invokeLater { updateUi(individualImageLabel, it, fitnessLabel) }
            }
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
