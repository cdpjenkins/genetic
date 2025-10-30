package com.cdpjenkins.genetic.evolver

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EvolverSettingsTest {

    @Test
    fun `default settings should have saveToFilesystem set to false`() {
        val settings = EvolverSettings.default("test-name")

        settings.saveToFilesystem shouldBe false
    }
}
