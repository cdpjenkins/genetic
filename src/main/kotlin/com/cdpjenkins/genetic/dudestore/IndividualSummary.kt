package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.model.Individual

data class IndividualSummary(
    val generation: Int,
    val fitness: Int,
    val timeInMillis: Long,
    val genomeSize: Int
) {
    companion object {
        fun of(individual: Individual): IndividualSummary {
            return IndividualSummary(
                individual.generation,
                individual.fitness,
                individual.timeInMillis,
                individual.genome.size
            )
        }
    }
}
