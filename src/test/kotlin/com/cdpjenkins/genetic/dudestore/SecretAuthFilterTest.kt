package com.cdpjenkins.genetic.dudestore

import io.kotest.matchers.shouldBe
import org.http4k.core.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SecretAuthFilterTest {

    private val next: HttpHandler = { Response(Status.OK) }
    private val filter = SecretAuthFilter(setOf("secret1", "secret2"))
    private val handler = filter.then(next)

    @Test
    fun `allows request through when secret matches first secret`() {
        val response = handler(Request(Method.GET, "/?secret=secret1"))
        response.status shouldBe Status.OK
    }

    @Test
    fun `allows request through when secret matches second secret`() {
        val response = handler(Request(Method.GET, "/?secret=secret2"))
        response.status shouldBe Status.OK
    }

    @Test
    fun `returns 401 when secret does not match any secret`() {
        val response = handler(Request(Method.GET, "/?secret=wrong"))
        response.status shouldBe Status.UNAUTHORIZED
    }

    @Test
    fun `throws when secret query param is missing`() {
        assertThrows<Exception> {
            handler(Request(Method.GET, "/"))
        }
    }
}
