package com.cdpjenkins.genetic.dudestore.client

import com.cdpjenkins.genetic.evolver.EvolverSettings
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.format.Jackson.auto

class DudeStoreClient(val baseUrl: String, val secret: String) {
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

    fun postMasterImage(name: String, masterImage: ByteArray): Status {
        val postResponse = httpClient(
            Request(Method.POST, "${baseUrl}/dudes/$name/master-image?secret=$secret")
                .header("Content-Type", "image/jpeg")
                .body(masterImage.inputStream())
        )

        if (postResponse.status != Status.OK) {
            logger.error { "Failed to POST master-image; status: ${postResponse.status}, body: ${postResponse.bodyString()}" }
        }

        return postResponse.status
    }

    fun getMasterImage(name: String): ByteArray? {
        val getResponse = httpClient(Request(Method.GET, "${baseUrl}/dudes/${name}/master-image?secret=$secret"))

        return if (getResponse.status == Status.OK) {
            getResponse.body.stream.readAllBytes()
        } else {
            null
        }
    }
}
