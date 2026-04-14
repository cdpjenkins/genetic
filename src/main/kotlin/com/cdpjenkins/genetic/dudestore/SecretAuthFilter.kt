package com.cdpjenkins.genetic.dudestore

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.core.*
import org.http4k.lens.Query

class SecretAuthFilter(val secrets: Set<String>) : Filter {
    val logger = KotlinLogging.logger {}

    override fun invoke(next: HttpHandler): HttpHandler {
        return { request: Request ->
            val secretLens = Query.required("secret")

            try {
                val actualSecret = secretLens(request)
                if (actualSecret in secrets) {
                    next(request)
                } else {
                    Response(Status.UNAUTHORIZED)
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to authenticate request" }
                throw e
            }
        }
    }
}

