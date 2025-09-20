package com.cdpjenkins.genetic

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class GeneticEvolverApplicationCommand : CliktCommand() {
    val name by argument()
    val masterImageFileName by option("--master-image")
    val secret by option(envvar = "SECRET").required()

    override fun run() {
        logger.info { "Starting application with name $name and master image $masterImageFileName" }

        val application = GeneticEvolverApplication.create(
            name, secret, masterImageFileName
        )
        application.start()
    }
}

fun main(args: Array<String>) = GeneticEvolverApplicationCommand().main(args)
