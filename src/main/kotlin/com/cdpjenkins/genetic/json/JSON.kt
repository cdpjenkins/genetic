package com.cdpjenkins.genetic.json

import com.cdpjenkins.genetic.model.Individual
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.InputStream

val mapper = jacksonObjectMapper()

fun <T> serialise(obj: T): String {
    return mapper.writeValueAsString(obj)
}

 inline fun <reified T> String.deserialise(): T {
    return mapper.readValue(this, T::class.java)
}

fun <T> serialiseToFile(file: File, obj: T) {
    mapper.writeValue(file, obj)
}

inline fun <reified T> deserialiseFromFile(jsonFile: File): T? =
    mapper.readValue(jsonFile, T::class.java)

fun fromStream(stream: InputStream): Individual = mapper.readValue(stream)

