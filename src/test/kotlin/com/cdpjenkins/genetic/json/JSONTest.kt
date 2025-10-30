package com.cdpjenkins.genetic.json

import com.cdpjenkins.genetic.evolver.EvolverSettings
import com.cdpjenkins.genetic.evolver.Weight
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
                "saveToS3": false,
                "weights": [
                    { "name": "QuadCurveShape", "weight": 1.0 },
                    { "name": "StrokedCubicCurveShape", "weight": 0.0 },
                    { "name": "Circle", "weight": 1.0 },
                    { "name": "RectangleShape", "weight": 0.0 },
                    { "name": "PolygonShape", "weight": 0.0 }
                ],
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
            version = 3,
            saveToS3 = false,
            weights = listOf(
                Weight("QuadCurveShape", 1.0),
                Weight("StrokedCubicCurveShape", 0.0),
                Weight("Circle", 1.0),
                Weight("RectangleShape", 0.0),
                Weight("PolygonShape", 0.0)
            )
        )
    }

    @Test
    fun `can deserialise StrokedCubicCurveShape without strokeWidth field`() {
        val jsonString = """
            {
                "type": "StrokedCubicCurveShape",
                "path": [
                    {"x": 0, "y": 0},
                    {"x": 10, "y": 20},
                    {"x": 30, "y": 40},
                    {"x": 50, "y": 60}
                ],
                "colour": {"red": 100, "green": 150, "blue": 200, "alpha": 255},
                "bounds": {"minX": 0, "minY": 0, "maxX": 100, "maxY": 100}
            }
        """.trimIndent()

        val shape = jsonString.deserialise<StrokedCubicCurveShape>()

        shape.strokeWidth shouldBe null
        shape.path shouldBe listOf(
            Point(0, 0),
            Point(10, 20),
            Point(30, 40),
            Point(50, 60)
        )
        shape.colour shouldBe Colour(100, 150, 200, 255)
        shape.bounds shouldBe BoundsRectangle(0, 0, 100, 100)
    }

    @Test
    fun `can serialise and deserialise StrokedCubicCurveShape with StrokeWidth`() {
        val shape = StrokedCubicCurveShape(
            path = listOf(
                Point(0, 0),
                Point(10, 20),
                Point(30, 40),
                Point(50, 60)
            ),
            colour = Colour(100, 150, 200, 255),
            bounds = BoundsRectangle(0, 0, 100, 100),
            strokeWidth = StrokeWidth(8.5f)
        )

        val jsonString = serialise(shape)
        val deserialized = jsonString.deserialise<StrokedCubicCurveShape>()

        deserialized shouldBe shape
        deserialized.strokeWidth?.value shouldBe 8.5f
    }
}
