package com.cdpjenkins.genetic.model.shape

import EvolverSettings
import com.cdpjenkins.genetic.model.withProbability
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.awt.Graphics2D

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = QuadCurveShape::class, name = "QuadCurveShape"),
    JsonSubTypes.Type(value = Circle::class, name = "Circle"),
    JsonSubTypes.Type(value = RectangleShape::class, name = "RectangleShape"),
    JsonSubTypes.Type(value = PolygonShape::class, name = "PolygonShape"),
)
interface Shape {
    fun draw(g: Graphics2D)
    fun mutate(evolverSettings: EvolverSettings): Shape
    fun maybeMutate(mutateProbability: Double, evolverSettings: EvolverSettings): Shape =
        if (withProbability(mutateProbability)) mutate(evolverSettings) else this
}
