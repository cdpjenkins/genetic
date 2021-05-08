package com.cdpjenkins.genetic.json

import com.cdpjenkins.genetic.model.Individual
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.InputStream

class JSON {
    val mapper = jacksonObjectMapper().also {
        // Yuck this is deprecated but I haven't yet figured out how to do this the proper way
        it.enableDefaultTyping()
    }

    fun serialise(individual: Individual): String {
        return mapper.writeValueAsString(individual)
    }

    fun deserialise(json: String): Individual {
        return mapper.readValue(json, Individual::class.java)
    }

    fun serialiseToFile(createTempFile: File, individual: Individual) {
        mapper.writeValue(createTempFile, individual)
    }

    fun deserialiseFromFile(jsonFile: File): Individual? =
        mapper.readValue(jsonFile, Individual::class.java)

    fun fromStream(stream: InputStream): Individual = mapper.readValue(stream)
}
