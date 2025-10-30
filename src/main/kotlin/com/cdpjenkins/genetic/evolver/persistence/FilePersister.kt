package com.cdpjenkins.genetic.evolver.persistence

import com.cdpjenkins.genetic.image.writePng
import com.cdpjenkins.genetic.json.serialiseToFile
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.svg.SvgRenderer
import java.io.File

class FilePersister(val name: String): Persister() {

    init {
        ensureDirExists("output")
        ensureDirExists("output/$name")
        ensureDirExists("output/$name/png")
        ensureDirExists("output/$name/json")
        ensureDirExists("output/$name/svg")
    }

    fun saveToDisk(individual: Individual) {
        if (individual.generation % 10 == 0) {
            val pngFileName = pngFileName(individual, name)
            writePng(individual, File("output/$pngFileName"))

            val jsonFileName = jsonFileName(individual, name)
            serialiseToFile(File("output/$jsonFileName"), individual)

            val svgFile = svgFileName(individual, name)
            SvgRenderer().renderToFile(File("output/$svgFile"), individual)
        }
    }
}