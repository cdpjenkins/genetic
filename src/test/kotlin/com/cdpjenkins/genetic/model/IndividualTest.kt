package com.cdpjenkins.genetic.model

import json.JSON
import org.junit.jupiter.api.Test
import java.io.File

internal class IndividualTest {

    private val json = JSON()

    private val individual: Individual = Individual(
        listOf(
            Circle(Point(0, 0), 10, Colour(1, 2, 3, 4), BoundsRectangle(0, 0, 100, 100)),
            RectangleShape(
                Point(0, 0),
                Point(100, 100),
                Colour(255, 255, 255, 255),
                BoundsRectangle(0, 0, 1000, 1000)
            )
        ),
        BoundsRectangle(0, 0, 100, 100)
    )

    @Test
    internal fun `can serialise and deserialise Individuals`() {
        val jsonString = json.serialise(individual)
        println(jsonString)
        val deserialise = json.deserialise(jsonString)

        // TODO probably can't do assertEquals on the resulting object unless we use data classes.
        // Which is probably a good idea.
    }

    @Test
    internal fun `can serialise to and deserialise from a file`() {
        val jsonFile = File.createTempFile("IndividualTest", ".json")
        json.serialiseToFile(jsonFile, individual)
        json.deserialiseFromFile(jsonFile)
    }
}
