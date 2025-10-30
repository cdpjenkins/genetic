package com.cdpjenkins.genetic.evolver.persistence

import com.cdpjenkins.genetic.image.writePng
import com.cdpjenkins.genetic.json.serialiseToFile
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.svg.SvgRenderer
import java.io.File

class FilePersister(): Persister() {

    init {
        ensureDirExists("output")
        ensureDirExists("output/png")
        ensureDirExists("output/json")
        ensureDirExists("output/svg")
    }

    fun saveToDisk(individual: Individual, name: String) {
        if (individual.generation % 10 == 0) {
            val pngFile = File(pngFileName(individual, name))
            writePng(individual, pngFile)

            val jsonFile = File(jsonFileName(individual, name))
            serialiseToFile(jsonFile, individual)

            SvgRenderer().renderToFile(
                File(svgFileName(individual, name)),
                individual
            )
        }
    }
}