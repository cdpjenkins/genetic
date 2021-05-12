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
            val dudePanel = JPanel()
            dudePanel.layout = BoxLayout(dudePanel, BoxLayout.X_AXIS)
            dudePanel.add(JLabel(masterIcon), BorderLayout.WEST)
            val individualImageLabel = JLabel(ImageIcon(masterImage))
            dudePanel.add(individualImageLabel, BorderLayout.CENTER)
            val diffImageLabel = JLabel(ImageIcon(masterImage))
            dudePanel.add(diffImageLabel, BorderLayout.EAST)

            val scrollPane = JScrollPane(dudePanel)
            add(scrollPane, BorderLayout.CENTER)

            val fitnessLabel = JLabel("", SwingConstants.RIGHT)
            add(fitnessLabel, BorderLayout.SOUTH)

            evolver.addListener {
                SwingUtilities.invokeLater { updateUi(individualImageLabel, it, fitnessLabel, diffImageLabel) }
            }
        }

        private fun updateUi(
            individualImageLabel: JLabel,
            it: Individual,
            fitnessLabel: JLabel,
            diffLabel: JLabel
        ) {
            individualImageLabel.icon = ImageIcon(it.bufferedImage)
            diffLabel.icon = ImageIcon(it.diffImage)
            fitnessLabel.text = it.describe()
            individualImageLabel.invalidate()
            repaint()
        }
    }
}
