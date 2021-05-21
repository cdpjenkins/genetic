package com.cdpjenkins.genetic.json

import com.cdpjenkins.genetic.model.Individual
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.InputStream

val mapper = jacksonObjectMapper()

fun serialise(individual: Individual): String {
    return mapper.writeValueAsString(individual)
}

fun String.deserialiseIndividual(): Individual {
    println("individual: ${this}" )
    println(this)
    return mapper.readValue(this, Individual::class.java)
}

fun serialiseToFile(createTempFile: File, individual: Individual) {
    mapper.writeValue(createTempFile, individual)
}

fun deserialiseFromFile(jsonFile: File): Individual? =
    mapper.readValue(jsonFile, Individual::class.java)

fun fromStream(stream: InputStream): Individual = mapper.readValue(stream)

