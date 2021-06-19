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
    val individualImageLabel: JLabel
    val fitnessLabel: JLabel

    init {
        val evolverPanel = JPanel(BorderLayout())

        val masterIcon = ImageIcon(masterImage)
        val dudePanel = JPanel()
        dudePanel.layout = BoxLayout(dudePanel, BoxLayout.X_AXIS)
        dudePanel.add(JLabel(masterIcon))

        individualImageLabel = JLabel(ImageIcon(masterImage))
        dudePanel.add(individualImageLabel)
        evolverPanel.add(dudePanel, BorderLayout.CENTER)

        fitnessLabel = JLabel("", SwingConstants.RIGHT)
        evolverPanel.add(fitnessLabel, BorderLayout.SOUTH)

        contentPane.add(evolverPanel)
        pack()
    }

    internal fun updateUiWithNewIndividual(it: Individual) {
        SwingUtilities.invokeLater {
            individualImageLabel.icon = ImageIcon(it.bufferedImage)
            fitnessLabel.text = it.describe()
            individualImageLabel.invalidate()
            pack()
            repaint()
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

    class EvolverPanel(
        masterImage: BufferedImage,
        evolver: Evolver
    ): JPanel(BorderLayout())
}
