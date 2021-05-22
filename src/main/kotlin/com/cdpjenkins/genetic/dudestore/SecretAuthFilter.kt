package com.cdpjenkins.genetic.dudestore

import org.http4k.core.*
import org.http4k.lens.Query

class SecretAuthFilter(val secret: String?) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler {
        return { request: Request ->
            val secretLens = Query.required("secret")

            try {
                val actualSecret = secretLens(request)
                if (actualSecret == secret) {
                    next(request)
                } else {
                    Response(Status.UNAUTHORIZED)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }
}

