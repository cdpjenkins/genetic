package com.cdpjenkins.genetic

import com.cdpjenkins.genetic.json.JSON
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.svg.SvgRenderer
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer

fun main(args: Array<String>) {
    val port = Integer.parseInt(args[0])
    val server = api.asServer(Netty(port)).start()
    server.block()
}

var currentDude: Individual? = null

private val json = JSON()

val api = routes(
        "/dude" bind Method.POST to {
            val newDude: Individual = json.fromStream(it.body.stream)
            currentDude = newDude
            Response(OK)
        },
        "/dude" bind Method.GET to {
            Response(OK)
                .body(SvgRenderer().renderToString(currentDude))
        }
    )
