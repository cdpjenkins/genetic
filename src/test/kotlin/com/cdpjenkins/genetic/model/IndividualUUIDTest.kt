package com.cdpjenkins.genetic.model

import com.cdpjenkins.genetic.evolver.EvolverSettings
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID

class IndividualUUIDTest {

    private val fixedUuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val anotherUuid = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val bounds = BoundsRectangle(0, 0, 100, 100)

    @Test
    fun `Individual stores the uuid it was given`() {
        val individual = Individual(emptyList(), bounds, uuid = fixedUuid)

        individual.uuid shouldBe fixedUuid
    }

    @Test
    fun `mutate uses the provided UUIDGenerationStrategy to produce a new uuid`() {
        val uuidStrategy = mockk<UUIDGenerationStrategy>()
        every { uuidStrategy.generate() } returns anotherUuid

        val original = Individual(emptyList(), bounds, uuid = fixedUuid)
        val mutated = original.mutate(EvolverSettings.default("test"), uuidStrategy)

        mutated.uuid shouldBe anotherUuid
    }

    @Test
    fun `mutated Individual has different uuid from original`() {
        val original = Individual(emptyList(), bounds, uuid = fixedUuid)

        val mutated = original.mutate(EvolverSettings.default("test"))

        mutated.uuid shouldNotBe original.uuid
    }

    @Test
    fun `two separately created Individuals have different uuids`() {
        val first = Individual(emptyList(), bounds)
        val second = Individual(emptyList(), bounds)

        first.uuid shouldNotBe second.uuid
    }
}
