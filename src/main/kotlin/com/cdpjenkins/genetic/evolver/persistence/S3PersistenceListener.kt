package com.cdpjenkins.genetic.evolver.persistence

import com.cdpjenkins.genetic.evolver.EvolverListener
import com.cdpjenkins.genetic.model.Individual

class S3PersistenceListener(private val persister: S3Persister): EvolverListener {
    override fun notify(individual: Individual) {
        if (individual.generation % 10 == 0) {
            persister.saveToS3(individual)
        }
    }
}

