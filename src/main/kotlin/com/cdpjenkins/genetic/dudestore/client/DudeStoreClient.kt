package com.cdpjenkins.genetic.dudestore.client

import EvolverSettings
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class BlockingDudeStoreClient(val baseUrl: String, val secret: String) {
    private val logger = KotlinLogging.logger {}

    private val httpClient = OkHttp()
    val individualLens = Body.auto<Individual>().toLens()
    val settingsLens = Body.auto<EvolverSettings>().toLens()

    fun getLatestDude(dudeName: String): Individual? {
        val response: Response =
            httpClient(Request(Method.GET, "$baseUrl/dudes/$dudeName/latest?type=json"))
        return if (response.status == Status.OK) {
            individualLens(response)
        } else {
            null
        }
    }

    fun postDude(it: Individual, dudeName: String): Status {
        val request: Request = Request(Method.POST, "$baseUrl/dudes/$dudeName?secret=${this.secret}")
            .body(serialise(it))
        val response = httpClient(request)
        if (response.status != Status.OK) {
            logger.error { "Failed to post new individual; status: ${response.status}, body: ${response.bodyString()}" }
        }

        return response.status
    }

    fun postSettings(name: String, evolverSettings: EvolverSettings): Status {
        val postResponse = httpClient(
            Request(Method.POST, "${baseUrl}/dudes/$name/settings?secret=$secret")
                .body(serialise(evolverSettings))
        )

        if (postResponse.status != Status.OK) {
            logger.error { "Failed to POST evolver settings; status: ${postResponse.status}, body: ${postResponse.bodyString()}" }
        }

        return postResponse.status
    }

    fun putSettings(
        name: String,
        settings: EvolverSettings
    ): Status {
        val putResponse = httpClient(
            Request(Method.PUT, "${baseUrl}/dudes/$name/settings?secret=$secret")
                .body(serialise(settings))
        )

        if (putResponse.status != Status.OK) {
            logger.error { "Failed to PUT evolver settings; status: ${putResponse.status}, body: ${putResponse.bodyString()}" }
        }

        return putResponse.status
    }


    fun getSettings(name: String): EvolverSettings? {
        val getResponse = httpClient(Request(Method.GET, "${baseUrl}/dudes/${name}/settings"))

        return if (getResponse.status == Status.OK) {
            settingsLens(getResponse)
        } else {
            null
        }
    }
}

// Tech debt: Not sure what I was thinking doing this with threads. This is Kotlin and coroutines are available.
// Future enhancement: turn these calls into something non-blocking that doesn't require firing up a whole thread
// for each HTTP request.
class NonBlockingDudeStoreClient(
    blockingDudeStoreClient: BlockingDudeStoreClient
)  {
    private val logger = KotlinLogging.logger {}
    private val blockingClient = blockingDudeStoreClient
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    fun getLatestDude(dudeName: String): Individual? {
        val future: Future<Individual?> = executorService.submit(Callable<Individual> {
            blockingClient.getLatestDude(dudeName)
        })

        return future.get()
    }

    fun postDude(it: Individual, dudeName: String) {
        logger.debug { "${"Posting new individual generation={}, fitness={}"} ${it.generation} ${it.fitness}" }

        executorService.submit {
            blockingClient.postDude(it, dudeName)
            logger.info { "Posted new individual successfully" }
        }
    }
}
