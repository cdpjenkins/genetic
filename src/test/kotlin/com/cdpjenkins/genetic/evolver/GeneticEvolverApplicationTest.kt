package com.cdpjenkins.genetic.evolver

import com.cdpjenkins.genetic.dudestore.client.DudeStoreClient
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test

class GeneticEvolverApplicationTest {

    private val bounds = BoundsRectangle(0, 0, 100, 100)
    private val currentIndividual = Individual(emptyList(), bounds, generation = 5)
    private val newerIndividual = Individual(emptyList(), bounds, generation = 10)
    private val olderIndividual = Individual(emptyList(), bounds, generation = 3)
    private val sameGenerationIndividual = Individual(emptyList(), bounds, generation = 5)

    private val evolver = mockk<Evolver>(relaxed = true)
    private val dudeStoreClient = mockk<DudeStoreClient>(relaxed = true)

    private val app = spyk(GeneticEvolverApplication("test", evolver, dudeStoreClient))

    @Test
    fun `should poll dudeStoreClient after 10 seconds`() {
        every { evolver.individual } returns currentIndividual
        every { dudeStoreClient.getLatestDude("test") } returns null

        app.checkForNewerIndividual(10_000)

        verify(exactly = 1) { dudeStoreClient.getLatestDude("test") }
    }

    @Test
    fun `should call notifyShouldDownloadNewIndividual when remote generation is higher`() {
        every { evolver.individual } returns currentIndividual
        every { dudeStoreClient.getLatestDude("test") } returns newerIndividual

        app.checkForNewerIndividual(10_000)

        verify(exactly = 1) { app.notifyShouldDownloadNewIndividual() }
    }

    @Test
    fun `should not call notifyShouldDownloadNewIndividual when remote generation is lower`() {
        every { evolver.individual } returns currentIndividual
        every { dudeStoreClient.getLatestDude("test") } returns olderIndividual

        app.checkForNewerIndividual(10_000)

        verify(exactly = 0) { app.notifyShouldDownloadNewIndividual() }
    }

    @Test
    fun `should not call notifyShouldDownloadNewIndividual when remote generation is same`() {
        every { evolver.individual } returns currentIndividual
        every { dudeStoreClient.getLatestDude("test") } returns sameGenerationIndividual

        app.checkForNewerIndividual(10_000)

        verify(exactly = 0) { app.notifyShouldDownloadNewIndividual() }
    }

    @Test
    fun `should not call notifyShouldDownloadNewIndividual when dudeStoreClient returns null`() {
        every { evolver.individual } returns currentIndividual
        every { dudeStoreClient.getLatestDude("test") } returns null

        app.checkForNewerIndividual(10_000)

        verify(exactly = 0) { app.notifyShouldDownloadNewIndividual() }
    }

    @Test
    fun `should not poll when less than 10 seconds have passed`() {
        val startTime = 0L
        every { evolver.individual } returns currentIndividual
        every { dudeStoreClient.getLatestDude("test") } returns null

        app.checkForNewerIndividual(startTime)
        app.checkForNewerIndividual(startTime + 5_000)

        verify(exactly = 1) { dudeStoreClient.getLatestDude(any()) }
    }

    @Test
    fun `should poll again after another 10 seconds`() {
        val startTime = 0L
        every { evolver.individual } returns currentIndividual
        every { dudeStoreClient.getLatestDude("test") } returns null

        app.checkForNewerIndividual(startTime)
        app.checkForNewerIndividual(startTime + 10_000)
        app.checkForNewerIndividual(startTime + 15_000)
        app.checkForNewerIndividual(startTime + 20_000)

        verify(exactly = 3) { dudeStoreClient.getLatestDude("test") }
    }
}
