package com.cdpjenkins.genetic.json

import com.cdpjenkins.genetic.model.*
import com.cdpjenkins.genetic.model.shape.*
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.io.File

internal class JSONTest {

    private val json = JSON()

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
        val jsonString = json.serialise(individual)
        val deserialisedIndividual: Individual = json.deserialise(jsonString)

        assertThat(deserialisedIndividual, equalTo(individual))
    }

    @Test
    internal fun `can serialise to and deserialise from a file`() {
        val jsonFile = File.createTempFile("IndividualTest", ".json")
        json.serialiseToFile(jsonFile, individual)
        val deserlialisedIndividual = json.deserialiseFromFile(jsonFile)

        assertThat(deserlialisedIndividual, equalTo(individual))
    }
}
