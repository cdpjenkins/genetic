package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.json.fromStream
import com.cdpjenkins.genetic.json.serialise
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
import org.jdbi.v3.core.Jdbi

fun main(args: Array<String>) {
    val port = Integer.parseInt(args[0])
    val dao = DudeDao(Jdbi
        .create(
            "jdbc:postgresql://localhost/dude_db",
            "test_user",
            "BUuVrC0QTe9A"
        )
    )

    val server = makeServer(port, System.getenv("SECRET"), dao)
    server.block()
}

fun makeServer(port: Int, secret: String, dudeDao: DudeDao): Http4kServer =
    makeApi(secret, dudeDao)
        .asServer(Netty(port))
        .start()

private fun makeApi(secret: String?, dao: DudeDao) = routes(
    "/dude" bind Method.POST to {
        if (secret != it.query("secret")) {
            Response(UNAUTHORIZED)
        } else {
            val newDude: Individual = fromStream(it.body.stream)
            dao.createTable()
            dao.insertDude(newDude)
            Response(OK)
        }
    },
    "/dude" bind Method.GET to {
        val currentDude = dao.latestDude()

        if (it.query("type") == "json") {
            currentDude?.serialiseToResponse() ?: Response(NOT_FOUND)
        } else {
            Response(OK)
                .body(SvgRenderer().renderToString(currentDude))
        }
    }
)

fun Individual.serialiseToResponse(): Response = Response(OK).body(serialise(this))
