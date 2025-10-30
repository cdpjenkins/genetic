package com.cdpjenkins.genetic.ui

import com.cdpjenkins.genetic.model.EvolverListener
import com.cdpjenkins.genetic.model.Individual
import java.awt.BorderLayout
import java.awt.image.BufferedImage
import javax.swing.*

class GUI(
    masterImage: BufferedImage
) : JFrame("Genetic!") {
    val individualImageLabel: JLabel
    val fitnessLabel: JLabel

    init {
        val evolverPanel = JPanel(BorderLayout()).also {
            val masterIcon = ImageIcon(masterImage)

            val dudePanel = JPanel().also {
                it.layout = BoxLayout(it, BoxLayout.X_AXIS)
                it.add(JLabel(masterIcon))

                individualImageLabel = JLabel(ImageIcon(masterImage))
                it.add(individualImageLabel)
            }
            it.add(dudePanel, BorderLayout.CENTER)

            fitnessLabel = JLabel("", SwingConstants.RIGHT)
            it.add(fitnessLabel, BorderLayout.SOUTH)
        }

        contentPane.add(evolverPanel)
        pack()
    }

    internal fun updateUiWithNewIndividual(it: Individual, name: String) {
        SwingUtilities.invokeLater {
            individualImageLabel.icon = ImageIcon(it.bufferedImage)
            fitnessLabel.text = it.describe(name)
            individualImageLabel.invalidate()
            pack()
            repaint()
        }
    }
}

class GUIEvolverListener(val gui: GUI, val name: String): EvolverListener {
    override fun notify(individual: Individual) {
        gui.updateUiWithNewIndividual(individual, name)
    }
}
