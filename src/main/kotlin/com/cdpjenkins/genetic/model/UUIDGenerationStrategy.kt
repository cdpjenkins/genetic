package com.cdpjenkins.genetic.model

import java.util.UUID

interface UUIDGenerationStrategy {
    fun generate(): UUID
}

class RandomUUIDGenerationStrategy : UUIDGenerationStrategy {
    override fun generate(): UUID = UUID.randomUUID()
}
