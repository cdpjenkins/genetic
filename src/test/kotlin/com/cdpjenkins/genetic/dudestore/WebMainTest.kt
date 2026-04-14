package com.cdpjenkins.genetic.dudestore

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class WebMainTest {
    @Test
    fun `splits secrets`() {
        val secrets = "secret1,secret2"
        val result = secrets.split(",")
        result shouldContainExactly setOf("secret1", "secret2")
    }
}
