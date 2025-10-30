package com.cdpjenkins.genetic.evolver

import EvolverSettings
import com.cdpjenkins.genetic.model.Individual

interface Mutator {
    fun mutate(individual: Individual): Individual
}

class MutatorImpl(val settings: EvolverSettings): Mutator {
    override fun mutate(individual: Individual): Individual = individual.mutate(settings)
}
