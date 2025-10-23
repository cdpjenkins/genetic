package com.cdpjenkins.genetic.json

import EvolverSettings
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.shape.*
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

internal class JSONTest {
    private val individual: Individual = Individual(
        listOf(
            Circle(
                Point(0, 0),
                10,
                Colour(1, 2, 3, 4),
                BoundsRectangle(0, 0, 100, 100)),
            RectangleShape(
                Point(0, 0),
                Point(100, 100),
                Colour(255, 255, 255, 255),
                BoundsRectangle(0, 0, 1000, 1000)
            ),
            QuadCurveShape(
                listOf(Point(0, 0), Point(10, 0), Point(10, 10), Point(10, 10)),
                Colour(255, 0, 0, 255),
                BoundsRectangle(0, 0, 200, 200)
            )
        ),
        BoundsRectangle(0, 0, 100, 100)
    )

    @Test
    internal fun `can serialise and deserialise Individuals`() {
        val jsonString = serialise(individual)

        jsonString.deserialise<Individual>() shouldBe individual

    }

    @Test
    internal fun `can serialise to and deserialise from a file`() {
        val jsonFile = File.createTempFile("IndividualTest", ".json")
        serialiseToFile(jsonFile, individual)

        deserialiseFromFile<Individual>(jsonFile) shouldBe individual
    }

    @Test
    fun `can deserialise EvolverSettings, even when it contains an unexpected field`() {
        val jsonString = """
            {
                "name": "cklr3",
                "initialGenomeSize": 0,
                "maxGenomeSize": 2000,
                "minAlpha": 32,
                "maxAlpha": 64,
                "colourMutateAmount": 8,
                "pointMutateRange": 3,
                "newShapeProbabilityFactor": 0.05,
                "avgShapesToMutate": 10.0,
                "version": 3,
                "unexpectedField": {
                    "something": 0,
                    "somethingElse": "hi"
                }
            }
        """.trimIndent()

        val settings = jsonString.deserialise<EvolverSettings>()

        settings shouldBe EvolverSettings(
            name = "cklr3",
            initialGenomeSize = 0,
            maxGenomeSize = 2000,
            minAlpha = 32,
            maxAlpha = 64,
            colourMutateAmount = 8,
            pointMutateRange = 3,
            newShapeProbabilityFactor = 0.05,
            avgShapesToMutate = 10.0,
            version = 3
        )
    }
}
