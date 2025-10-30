package com.cdpjenkins.genetic.evolver

import EvolverSettings
import com.cdpjenkins.genetic.model.Individual

class Mutator(val settings: EvolverSettings) {
    fun mutate(individual: Individual): Individual = individual.mutate(settings)
}
