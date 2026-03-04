package com.cdpjenkins.genetic.model.shape

import com.cdpjenkins.genetic.evolver.EvolverSettings
import com.cdpjenkins.genetic.json.deserialise
import com.cdpjenkins.genetic.json.serialise
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class UnboundedQuadCurveShapeTest {

    val evolverSettings = EvolverSettings(
        name = "test",
        initialGenomeSize = 1,
        maxGenomeSize = 10,
        minAlpha = 32,
        maxAlpha = 64,
        colourMutateAmount = 8,
        pointMutateRange = 3,
        newShapeProbabilityFactor = 0.05,
        avgShapesToMutate = 10.0,
        version = 1,
        saveToS3 = false,
        weights = null
    )

    val shape = UnboundedQuadCurveShape(
        quadCurvePath = listOf(Point(0, 0), Point(10, 0), Point(10, 10), Point(0, 10)),
        quadCurveColour = Colour(255, 0, 0, 128)
    )

    @Test
    fun `mutate returns an UnboundedQuadCurveShape`() {
        val mutated = shape.mutate(evolverSettings)

        mutated.shouldBeInstanceOf<UnboundedQuadCurveShape>()
    }

    @Test
    fun `can serialise and deserialise UnboundedQuadCurveShape`() {
        val json = serialise(shape)
        val deserialized = json.deserialise<UnboundedQuadCurveShape>()

        deserialized shouldBe shape
    }
}
