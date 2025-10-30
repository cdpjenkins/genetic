package com.cdpjenkins.genetic.evolver

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class Weight(val name: String, val weight: Double)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EvolverSettings(
    val name: String,
    val initialGenomeSize: Int,
    val maxGenomeSize: Int,
    val minAlpha: Int,
    val maxAlpha: Int,
    val colourMutateAmount: Int,
    val pointMutateRange: Int,
    val newShapeProbabilityFactor: Double,
    val avgShapesToMutate: Double,
    val version: Int? = null,
    val saveToS3: Boolean? = true,
    val saveToFilesystem: Boolean? = false,
    val minStrokeWidth: Float? = null,
    val maxStrokeWidth: Float? = null,
    val weights: List<Weight>? = null
) {
    companion object {
        fun default(name: String) = EvolverSettings(
            name,
            0,
            1000,
            32,
            64,
            8,
            3,
            0.005,
            10.0,
            1,
            false,
            false,
            4f,
            32f
        )
    }
}
