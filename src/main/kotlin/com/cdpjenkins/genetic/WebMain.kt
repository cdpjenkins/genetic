package com.cdpjenkins.genetic

import com.cdpjenkins.genetic.json.JSON
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.svg.SvgRenderer
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer

fun main(args: Array<String>) {
    val port = Integer.parseInt(args[0])
    val server = makeServer(port, System.getenv("SECRET"))
    server.block()
}

internal fun makeServer(port: Int, secret: String): Http4kServer =
    makeApi(secret).asServer(Netty(port)).start()

var currentDude: Individual? = null

private val json = JSON()

private fun makeApi(secret: String?) = routes(
    "/dude" bind Method.POST to {
        if (secret != it.query("secret")) {
            Response(UNAUTHORIZED)
        } else {
            val newDude: Individual = json.fromStream(it.body.stream)
            currentDude = newDude
            Response(OK)
        }
    },
    "/dude" bind Method.GET to {
        if (it.query("type") == "json") {
                currentDude?.serialiseToResponse() ?: Response(NOT_FOUND)
        } else {
            Response(OK)
                .body(SvgRenderer().renderToString(currentDude))
        }
    }
)

fun Individual.serialiseToResponse(): Response = Response(OK).body(json.serialise(this))
