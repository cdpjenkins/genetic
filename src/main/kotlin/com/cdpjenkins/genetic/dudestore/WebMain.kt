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

class DudeStoreApplication(val dao: DudeDao, val port: Int, val secret: String) {
    val typeLens = Query.optional("type")
    val individualLens = Body.auto<Individual>().toLens()
    val individualSummaryLens = Body.auto<IndividualSummary>().toLens()
    val dudeSummariesLens = Body.auto<DudeSummaryList>().toLens()
    val nameLens = Path.string().of("name")

    fun startServer(): Http4kServer =
        ExceptionLoggingFilter
            .then(makeApi(secret))
            .asServer(Netty(port))
            .start()

    private fun makeApi(secret: String?): RoutingHttpHandler =
        routes(
            // mega dangerous stuff that maybe shouldn't be there
            "setup" bind Method.POST to SecretAuthFilter(secret).then(::setupHandler),
            "recreate" bind Method.POST to SecretAuthFilter(secret).then(::recreateHandler),

            // endpoints that we want to use
            "/dudes" bind Method.GET to ::getDudesList,
            "/dudes/{name}" bind Method.POST to SecretAuthFilter(secret).then(::postDudeHandler),
            "/dudes/{name}/latest" bind Method.GET to ::getDudeLatest,
            "/dudes/{name}/latest/summary" bind Method.GET to ::getDudeLatestSummary,

            // legacy endpoints that we should stop using
            "/dude/{name}" bind Method.POST to SecretAuthFilter(secret).then(::postDudeHandler),
            "/dude/{name}/latest" bind Method.GET to ::getDudeLatest,
            "/dude/{name}/latest/summary" bind Method.GET to ::getDudeLatestSummary,
        )

    @Suppress("UNUSED_PARAMETER")
    private fun setupHandler(request: Request): Response {
        dao.createTable()
        return Response(OK)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun recreateHandler(request: Request): Response {
        dao.recreate()
        return Response(OK)
    }

    private fun postDudeHandler(request: Request): Response {
        val name = nameLens(request)
        val newDude: Individual = individualLens(request)
        dao.createTable()
        dao.insertDude(newDude, name, newDude.generation)
        return Response(OK)
    }

    private fun getDudeLatest(request: Request): Response {
        val currentDude = dao.latestDude(nameLens(request))

        return if (typeLens(request) == "json") {
            if (currentDude != null) {
                Response(OK).with(individualLens of currentDude)
            } else {
                Response(NOT_FOUND)
            }
        } else {
            Response(OK)
                .body(SvgRenderer().renderToString(currentDude))
        }
    }

    private fun getDudeLatestSummary(request: Request): Response {
        val currentDude = dao.latestDude(nameLens(request))

        return if (currentDude != null) {
            Response(OK)
                .with(individualSummaryLens of IndividualSummary.of(currentDude))
        } else {
            Response(NOT_FOUND)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getDudesList(request: Request): Response {
        val dudeSummaries = dao.listDudeSummaries()

        return Response(OK).with(dudeSummariesLens of dudeSummaries)
    }
}

fun main() {
    val defaultConfig = Environment.from(

    )

    val environment = Environment.ENV
        .overrides(defaultConfig)

    val portLens: BiDiLens<Environment, Int> = EnvironmentKey.int().required("PORT")
    val secretLens = EnvironmentKey.string().required("SECRET")
    val jdbcDatabaseUrlLens = EnvironmentKey.string().required("JDBC_DATABASE_URL")

    val port = portLens(environment)
    val secret = secretLens(environment)

    val dao = DudeDao(Jdbi.create(jdbcDatabaseUrlLens(environment)))

    DudeStoreApplication(dao, port, secret)
        .startServer()
        .also { it.block() }
}
