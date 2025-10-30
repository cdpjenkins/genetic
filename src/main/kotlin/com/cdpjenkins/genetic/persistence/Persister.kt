package com.cdpjenkins.genetic.persistence

import com.cdpjenkins.genetic.model.Individual

abstract class Persister {
    protected fun svgFileName(individual: Individual, name: String) =
        String.format("$name/svg/cow_%010d.svg", individual.generation)

    protected fun jsonFileName(individual: Individual, name: String) =
        String.format("$name/json/cow_%010d.json", individual.generation)

    protected fun pngFileName(individual: Individual, name: String) =
        String.format("$name/png/cow_%010d.png", individual.generation)
}

