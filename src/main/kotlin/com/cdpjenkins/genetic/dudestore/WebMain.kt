package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.svg.SvgRenderer
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.*
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.auto
import org.http4k.lens.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.jdbi.v3.core.Jdbi

fun main(args: Array<String>) {

    val defaultConfig = Environment.from(

    )

    val environment = Environment.ENV
        .overrides(defaultConfig)

    val portLens: BiDiLens<Environment, Int> = EnvironmentKey.int().required("PORT")
    val secretLens = EnvironmentKey.string().required("SECRET")
    val jdbcDatabaseUrlLens = EnvironmentKey.string().required("JDBC_DATABASE_URL")

    val dao = DudeDao(Jdbi.create(jdbcDatabaseUrlLens(environment)))
    val server = makeServer(portLens(environment), secretLens(environment), dao)
    server.block()
}

fun makeServer(port: Int, secret: String, dudeDao: DudeDao): Http4kServer {
    return ExceptionLoggingFilter
        .then(makeApi(secret, dudeDao))
        .asServer(Netty(port))
        .start()
}

private fun makeApi(secret: String?, dao: DudeDao): RoutingHttpHandler {
    val typeLens = Query.optional("type")
    val individualLens = Body.auto<Individual>().toLens()
    val individualSummaryLens = Body.auto<IndividualSummary>().toLens()
    val dudeSummariesLens = Body.auto<DudeSummaryList>().toLens()
    val nameLens = Path.string().of("name")

    return routes(
        "setup" bind Method.POST to SecretAuthFilter(secret).then { request: Request ->
            dao.createTable()
            Response(OK)
        },
        "recreate" bind Method.POST to SecretAuthFilter(secret).then { request: Request ->
            dao.recreate()
            Response(OK)
        },
        "/dude/{name}" bind Method.POST to SecretAuthFilter(secret).then { request: Request ->
            val name = nameLens(request)
            val newDude: Individual = individualLens(request)
            dao.createTable()
            dao.insertDude(newDude, name, newDude.generation)
            Response(OK)
        },
        "/dude/{name}/latest" bind Method.GET to { request: Request ->
            val currentDude = dao.latestDude(nameLens(request))

            if (typeLens(request) == "json") {
                if (currentDude != null) {
                    Response(OK).with(individualLens of currentDude)
                } else {
                    Response(NOT_FOUND)
                }
            } else {
                Response(OK)
                    .body(SvgRenderer().renderToString(currentDude))
            }
        },
        "/dude/{name}/latest/summary" bind Method.GET to { request: Request ->
            val currentDude = dao.latestDude(nameLens(request))

            if (currentDude != null) {
                Response(OK)
                    .with(individualSummaryLens of IndividualSummary.of(currentDude))
            } else {
                Response(NOT_FOUND)
            }
        },
        "dudes" bind Method.GET to { request: Request ->
            val dudeSummaries = dao.listDudeSummaries()

            Response(OK).with(dudeSummariesLens of dudeSummaries)
        }
    )
}
