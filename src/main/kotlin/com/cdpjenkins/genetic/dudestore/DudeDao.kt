package com.cdpjenkins.genetic.dudestore

import EvolverSettings
import com.cdpjenkins.genetic.json.deserialise
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jdbi.v3.core.Jdbi

class DudeDao(val jdbi: Jdbi) {
    val logger = KotlinLogging.logger {}

    fun createTables() {
        jdbi.withHandle<Int, Exception> {
            it.execute(
                """
                CREATE TABLE IF NOT EXISTS Dudes(
                    name VARCHAR(50) NOT NULL,
                    generation INT NOT NULL,
                    individual JSONB NOT NULL,
                    PRIMARY KEY (name, generation)
                );
                """.trimIndent())
        }

        jdbi.withHandle<Int, Exception> {
            it.execute(
                """
                CREATE TABLE IF NOT EXISTS EvolverSettings(
                    name VARCHAR PRIMARY KEY,
                    settings JSONB NOT NULL
                );
                """.trimIndent())
        }
    }

    fun recreate() {
        jdbi.withHandle<Int, Exception> {
            it.execute(
                """
                DROP TABLE IF EXISTS Dudes;
                """.trimIndent()
            )
        }
        createTables()
    }

    fun insertDude(dude: Individual, name: String, generation: Int) {
        val serialisedIndividual = serialise(dude)
        logger.debug { "Serialised individual: $serialisedIndividual" }

        jdbi.withHandle<Int, Exception> {
            it.createUpdate(
                """
                    INSERT INTO Dudes (name, generation, individual)
                    VALUES(:name, :generation, cast (:individual as JSONB))
                """.trimIndent()
            )
                .bind("name", name)
                .bind("generation", generation)
                .bind("individual", serialisedIndividual)
                .execute()
        }
    }

    fun latestDude(name: String): Individual? {
        try {
            return jdbi.withHandle<Individual, Exception> {
                it.createQuery(
                    """
                        SELECT individual FROM dudes
                        WHERE name=:name
                        ORDER BY generation DESC LIMIT 1
                    """.trimIndent()
                )
                    .bind("name", name)
                    .mapToBean(Dude::class.java)
                    .findOne()
                    .orElse(null)
                    ?.individual
                    ?.deserialise()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun listDudeSummaries(): DudeSummaryList {
        return try {
            jdbi.withHandle<DudeSummaryList, Exception> {
                val summaries = it.createQuery(
                    """
                        SELECT name, count(*) as numGenerations FROM dudes
                        GROUP BY name
                    """.trimIndent()
                )
                    .mapToBean(DudeSummary::class.java)
                    .list()
                DudeSummaryList(summaries)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun insertEvolverSettings(evolverSettings: EvolverSettings) {
        jdbi.withHandle<Int, Exception> {
            it.createUpdate(
                """
                    INSERT INTO EvolverSettings (name, settings)
                    VALUES(:name, cast (:settings as JSONB))
                """.trimIndent()
            )
                .bind("name", evolverSettings.name)
                .bind("settings", serialise(evolverSettings))
                .execute()
        }
    }

    fun getEvolverSettings(name: String): EvolverSettings? {
        return jdbi.withHandle<EvolverSettings, Exception> {
            it.createQuery(
                """
                    SELECT name, settings FROM EvolverSettings
                    WHERE name=:name
                """.trimIndent()
            )
                .bind("name", name)
                .mapToBean(EvolverSettingsRow::class.java)
                .findOne()
                .orElse(null)
                ?.settings
                ?.deserialise()
        }
    }
}

data class Dude(
    var id: Int,
    var individual: String?
) {
    constructor() : this(0, null)
}

data class EvolverSettingsRow(
    var name: String,
    var settings: String?
) {
    constructor() : this("", null)
}