package com.cdpjenkins.genetic

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger(object{}::class.java)

fun main(args: Array<String>) {
    val secret = System.getenv("SECRET")
    val name = args[0]

    val application = GeneticApplication.create(name, secret, args[1])
    application.start()
}
