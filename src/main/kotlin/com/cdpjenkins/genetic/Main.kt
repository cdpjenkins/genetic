package com.cdpjenkins.genetic

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val secret = System.getenv("SECRET")
    val name = args[0]
    val masterImageFileName = args[1]

    logger.info { "Starting application with name $name and master image $masterImageFileName" }

    val application = GeneticApplication.create(
        name, secret, masterImageFileName
    )
    application.start()
}
