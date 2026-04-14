package com.cdpjenkins.genetic.dudestore

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class WebMainTest {
    @Test
    fun `splits secrets`() {
        splitSecrets("secret1,secret2") shouldContainExactly setOf("secret1", "secret2")
    }
}
