package com.cdpjenkins.genetic.dudestore.client

import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class DudeStoreClient(val baseUrl: String, val dudeName: String, val secret: String) {
    private val httpClient = OkHttp()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    val individualLens = Body.auto<Individual>().toLens()

    fun getLatestDude(): Individual? {
        val future: Future<Individual?> = executorService.submit(Callable<Individual> {
            val response: Response =
                httpClient(Request(Method.GET, "$baseUrl/dudes/${dudeName}/latest?type=json"))
            if (response.status == Status.OK) {
                individualLens(response)
            } else {
                null
            }
        })

        return future.get()
    }

    fun postDude(it: Individual) {
        executorService.submit {
            val request: Request = Request(Method.POST, "$baseUrl/dudes/${dudeName}?secret=${this.secret}")
                .body(serialise(it))
            val response = httpClient(request)
            if (response.status != Status.OK) {
                println(response)
            }
        }
    }
}
