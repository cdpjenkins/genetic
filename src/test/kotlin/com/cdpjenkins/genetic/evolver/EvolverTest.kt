package com.cdpjenkins.genetic.evolver

import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class EvolverTest {

    val mutator = mockk<Mutator>()
    val listener = mockk<EvolverListener>(relaxed = true)

    val masterImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)

    val currentIndividual = individualWithFitness(1000)
    val betterIndividual = individualWithFitness(800)
    val sameIndividual = individualWithFitness(1000)
    val worseIndividual = individualWithFitness(1200)

    val evolver = Evolver("test", currentIndividual, masterImage, mutator)
        .also { it.addListener(listener) }

    @Test
    fun `accepts mutation when fitness improves and notifies listeners`() {
        every { mutator.mutate(any()) } returns betterIndividual

        evolver.mutateOnce()

        verify(exactly = 1) { listener.notify(betterIndividual) }
    }

    @Test
    fun `rejects mutation when fitness gets worse and does not notify listeners`() {
        every { mutator.mutate(any()) } returns worseIndividual

        evolver.mutateOnce()

        verify(exactly = 0) { listener.notify(any()) }
    }

    @Test
    fun `rejects mutation when fitness stays the same and does not notify listeners`() {
        every { mutator.mutate(any()) } returns sameIndividual

        evolver.mutateOnce()

        verify(exactly = 0) { listener.notify(any()) }
    }

    private fun individualWithFitness(fitness: Int): Individual {
        val individual = spyk(Individual(
            emptyList(),
            BoundsRectangle(0, 0, 10, 10),
            1
        ))
        individual.fitness = fitness
        every { individual.drawAndCalculateFitness(any()) } answers {
            individual.fitness = fitness
        }
        return individual
    }
}
