package com.cdpjenkins.genetic.util

import com.cdpjenkins.genetic.evolver.Weight
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WeightedSelectorTest {
    @Test
    fun `chooses one option when that's the only option there is`() {
        val weightedSelector = WeightedSelector(
            randomSource = willReturn(0.5)
        )

        weightedSelector.select(
            listOf(Weight("OnlyOption", 1.0))
        ) shouldBe "OnlyOption"
    }

    @Test
    fun `returns only option with non-zero weight when that's all there is`() {
        val weightedSelector = WeightedSelector(
            randomSource = willReturn(0.5)
        )

        weightedSelector.select(
            listOf(
                Weight("TheOnlyOne", 1.0),
                Weight("SomethingElse", 0.0)
            )
        ) shouldBe "TheOnlyOne"
    }

    @Test
    fun `will return first option if random source gives number in that range`() {
        val weightedSelector = WeightedSelector(
            randomSource = willReturn(0.5)
        )

        weightedSelector.select(
            listOf(
                Weight("TheFirstOne", 1.0),
                    Weight("TheSecondOne", 1.0)
            )
        ) shouldBe "TheFirstOne"
    }

    @Test
    fun `will return second option if random source gives number in its range`() {
        val weightedSelector = WeightedSelector(
            randomSource = willReturn(1.5)
        )

        weightedSelector.select(
            options = listOf(
                Weight("TheFirstOne", 1.0),
                Weight("TheSecondOne", 1.0)
            )
        ) shouldBe "TheSecondOne"
    }

    @Test
    fun `will throw IllegalArgumentException if weights list is empty`() {
        val weightedSelector = WeightedSelector(
            randomSource = willReturn(0.5)
        )

        assertThrows<IllegalArgumentException> {
            weightedSelector.select(emptyList())
        }.message shouldBe "options list must not be empty"
    }
}

class PartialSumsTest {
    @Test
    fun `partial sums of empty list is empty`() {
        partialSumsOf(emptyList()) shouldBe emptyList()
    }

    @Test
    fun `partial sums of list with one element is a list with just that element`() {
        partialSumsOf(listOf(1.0)) shouldBe listOf(1.0)
    }

    @Test
    fun `partial sums of list with multiple elements should be actual partial sums`() {
        partialSumsOf(listOf(1.0, 2.0, 3.0, 4.0)) shouldBe listOf(1.0, 3.0, 6.0, 10.0)
    }
}

fun willReturn(hardcodedValue: Double) = RandomSource { hardcodedValue }
