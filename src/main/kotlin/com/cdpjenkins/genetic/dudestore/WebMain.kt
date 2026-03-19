package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.evolver.EvolverSettings
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.svg.SvgRenderer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.config.Environment
import org.http4k.config.EnvironmentKey
import org.http4k.core.*
import org.http4k.core.Status.Companion.CONFLICT
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
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class DudeStoreApplication(
    val dao: DudeDao,
    val port: Int,
    val s3Persister: DudeStoreS3Persister? = null,
    val secrets: Set<String>
) {
    private val logger = KotlinLogging.logger {}

    val typeLens = Query.optional("type")
    val individualLens = Body.auto<Individual>().toLens()
    val individualSummaryLens = Body.auto<IndividualSummary>().toLens()
    val dudeSummariesLens = Body.auto<DudeSummaryList>().toLens()
    val nameLens = Path.string().of("name")
    val generationLens = Path.int().of("generation")
    val evolveSettingsLens = Body.auto<EvolverSettings>().toLens()

    fun startServer(): Http4kServer {
        val secretAuthFilter = SecretAuthFilter(secrets)

        return ExceptionLoggingFilter
            .then(makeApi(secretAuthFilter))
            .asServer(Netty(port))
            .start()
    }

    private fun makeApi(secretAuthFilter: SecretAuthFilter): RoutingHttpHandler =
        ExceptionHandlingFilter.then(
            routes(
                // mega dangerous stuff that maybe shouldn't be there
                "setup" bind Method.POST to secretAuthFilter.then(::setupHandler),
                "recreate" bind Method.POST to secretAuthFilter.then(::recreateHandler),

                // endpoints that we want to use
                "/dudes" bind Method.GET to ::getDudesListHandler,
                "/dudes/{name}" bind Method.POST to secretAuthFilter.then(::postDudeHandler),
                "/dudes/{name}/latest" bind Method.GET to ::getDudeLatest,
                "/dudes/{name}/latest/summary" bind Method.GET to ::getDudeLatestSummary,
                "/dudes/{name}/settings" bind Method.POST to secretAuthFilter.then(::postEvolverSettingsHandler),
                "/dudes/{name}/settings" bind Method.PUT to secretAuthFilter.then(::putEvolverSettingsHandler),
                "/dudes/{name}/settings" bind Method.GET to ::getEvolverSettingsHandler,
                "/dudes/{name}/master-image" bind Method.POST to secretAuthFilter.then(::postMasterImageHandler),
                "/dudes/{name}/master-image" bind Method.GET to secretAuthFilter.then(::getMasterImageHandler),
                "/dudes/{name}/{generation}" bind Method.GET to ::getDudeByGeneration
            )
        )

    @Suppress("UNUSED_PARAMETER")
    private fun setupHandler(_request: Request): Response {
        dao.createTables()
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
        dao.createTables()
        dao.insertDude(newDude, name, newDude.generation)
        s3Persister?.saveToS3(newDude, name)
        return Response(OK)
    }

    private fun resolveIndividual(dude: Dude?): Individual? {
        if (dude == null) return null
        return dude.individual ?: s3Persister?.readFromS3(dude.name, dude.generation)
    }

    private fun getDudeLatest(request: Request): Response {
        val currentDude = dao.latestDude(nameLens(request))
        val individual = resolveIndividual(currentDude)

        return when (typeLens(request)) {
            "json" -> {
                if (individual != null) {
                    Response(OK).with(individualLens of individual)
                } else {
                    Response(NOT_FOUND)
                }
            }
            "png" -> {
                if (individual != null) {
                    individual.drawToBuffer()
                    val outputStream = ByteArrayOutputStream()
                    ImageIO.write(individual.bufferedImage, "png", outputStream)
                    Response(OK)
                        .header("Content-Type", "image/png")
                        .body(outputStream.toByteArray().inputStream())
                } else {
                    Response(NOT_FOUND)
                }
            }
            else -> {
                Response(OK)
                    .body(SvgRenderer().renderToString(individual))
            }
        }
    }

    private fun getDudeLatestSummary(request: Request): Response {
        val currentDude = dao.latestDude(nameLens(request)) ?: return Response(NOT_FOUND)
        val individual by lazy { resolveIndividual(currentDude)!! }
        return Response(OK).with(individualSummaryLens of IndividualSummary(
            generation = currentDude.generation,
            fitness = currentDude.fitness ?: individual.fitness,
            timeInMillis = currentDude.timeInMillis ?: individual.timeInMillis,
            genomeSize = currentDude.genomeSize ?: individual.genome.size,
            timestamp = currentDude.createdTimestamp ?: individual.createdTimestamp,
            uuid = currentDude.uuid ?: individual.uuid
        ))
    }

    private fun getDudeByGeneration(request: Request): Response {
        val name = nameLens(request)
        val generation = generationLens(request)
        val currentDude = dao.dudeByGeneration(name, generation)
        val individual = resolveIndividual(currentDude)

        return when (typeLens(request)) {
            "json" -> {
                if (individual != null) {
                    Response(OK).with(individualLens of individual)
                } else {
                    Response(NOT_FOUND)
                }
            }
            "png" -> {
                if (individual != null) {
                    individual.drawToBuffer()
                    val outputStream = ByteArrayOutputStream()
                    ImageIO.write(individual.bufferedImage, "png", outputStream)
                    Response(OK)
                        .header("Content-Type", "image/png")
                        .body(outputStream.toByteArray().inputStream())
                } else {
                    Response(NOT_FOUND)
                }
            }
            else -> {
                Response(OK)
                    .body(SvgRenderer().renderToString(individual))
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getDudesListHandler(request: Request): Response {
        val dudeSummaries = dao.listDudeSummaries()

        return Response(OK).with(dudeSummariesLens of dudeSummaries)
    }

    private fun postMasterImageHandler(request: Request): Response {

        // Note: this is going to allow someone to DDOS us horribly by sending zillions of bytes, so we'd better figure
        // out a way to avoid that at some point...
        //
        // Not just this endpoint, any of the POST/PUT endpoints :-/

        val name = nameLens(request)
        logger.info { "POST master-image for $name with length: ${request.body.length} bytes" }

        val masterImageBytes = request.body.stream.readBytes()

        dao.insertMasterImage(name, masterImageBytes)
        return Response(OK)
    }

    private fun getMasterImageHandler(request: Request): Response {
        val masterImageBytes = dao.getMasterImage(nameLens(request))

        return if (masterImageBytes != null) {
            Response(OK)
                .header("Content-Type", "image/jpeg")
                .body(masterImageBytes.inputStream())
        } else {
            Response(NOT_FOUND)
        }
    }


    private fun putEvolverSettingsHandler(request: Request): Response {
        val name = nameLens(request)
        val evolverSettings: EvolverSettings = evolveSettingsLens(request)

        logger.info { "PUT evolver settings for $name: $evolverSettings" }

        // TODO if the name doesn't match, return HTTP 400

        dao.updateEvolverSettings(evolverSettings)
        return Response(OK)
    }

    private fun getEvolverSettingsHandler(request: Request): Response {
        val name = nameLens(request)
        val evolverSettings = dao.getEvolverSettings(name)

        return evolverSettings?.let {
            Response(OK).with(evolveSettingsLens of it)
        } ?: Response(NOT_FOUND)
    }

    private fun postEvolverSettingsHandler(request: Request): Response {
        val name = nameLens(request)
        val evolverSettings: EvolverSettings = evolveSettingsLens(request)

        logger.info { "POST evolver settings for $name: $evolverSettings" }

        // TODO if the name doesn't match, return HTTP 400

        dao.insertEvolverSettings(evolverSettings)
        return Response(OK)
    }
}

fun main() {
    val defaultConfig = Environment.from(

    )

    val environment = Environment.ENV
        .overrides(defaultConfig)

    val portLens: BiDiLens<Environment, Int> = EnvironmentKey.int().required("PORT")
    val jdbcDatabaseUrlLens = EnvironmentKey.string().required("JDBC_DATABASE_URL")

    val port = portLens(environment)
    val secret = EnvironmentKey.string().optional("SECRET")(environment)
    val secret2 = EnvironmentKey.string().optional("SECRET2")(environment)
    val secrets = (EnvironmentKey.string().optional("SECRETS")(environment) ?: "")
        .split(",")
        .plus(secret)
        .plus(secret2)
        .filterNotNull()
        .filter { it.isNotEmpty() }
        .toSet()

    val dao = DudeDao(Jdbi.create(jdbcDatabaseUrlLens(environment)))

    val s3Client = S3Client.builder().region(Region.EU_WEST_2).build()
    val s3Persister = DudeStoreS3Persister(s3Client, "com-cdpjenkins-genetic-assets")

    DudeStoreApplication(dao, port, s3Persister, secrets)
        .startServer()
        .also { it.block() }
}

object ExceptionHandlingFilter: Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request: Request ->
        try {
            next(request)
        } catch (_: AlreadyExistsException) {
            Response(CONFLICT)
        }
    }
}
