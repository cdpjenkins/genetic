package com.cdpjenkins.genetic.dudestore

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request

object ExceptionLoggingFilter: Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request: Request ->
        try {
            next(request)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
