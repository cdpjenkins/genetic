package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.model.Individual
import java.util.UUID

data class IndividualSummary(
    val generation: Int,
    val fitness: Int,
    val timeInMillis: Long,
    val genomeSize: Int,
    val timestamp: Long,
    val uuid: UUID
) {
    companion object {
        fun of(individual: Individual): IndividualSummary {
            return IndividualSummary(
                individual.generation,
                individual.fitness,
                individual.timeInMillis,
                individual.genome.size,
                individual.createdTimestamp,
                individual.uuid
            )
        }
    }
}
