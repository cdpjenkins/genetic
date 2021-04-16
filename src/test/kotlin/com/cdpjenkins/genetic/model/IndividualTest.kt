package com.cdpjenkins.genetic.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test

internal class IndividualTest {

    @Test
    internal fun `can serialise and deserialise Individuals`() {
        val mapper = jacksonObjectMapper()
        // Yuck this is deprecated but I haven't yet figured out how to do this the proper way
        mapper.enableDefaultTyping()

        val individual = Individual(
            listOf(
                Circle(Point(0, 0), 10, Colour(1, 2, 3, 4), BoundsRectangle(0, 0, 100, 100))),
                BoundsRectangle(0, 0, 100, 100)
            )

        val json = mapper.writeValueAsString(individual)
        println(json)
        mapper.readValue(json, Individual::class.java)
    }
}