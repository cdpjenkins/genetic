package com.cdpjenkins.genetic.dudestore.client

import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request

class DudeClient(val baseUrl: String, val dudeName: String, val secret: String) {
    private val httpClient = OkHttp()

    fun getLatestDude() =
        httpClient(Request(Method.GET, "$baseUrl/dude/${dudeName}?type=json"))

    fun postDude(it: Individual) = httpClient(
        Request(Method.POST, "$baseUrl/dude/${dudeName}?secret=${this.secret}")
            .body(serialise(it))
    )
}
