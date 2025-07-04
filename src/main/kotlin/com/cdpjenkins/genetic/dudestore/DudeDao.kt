package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.json.deserialiseIndividual
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import org.jdbi.v3.core.Jdbi

class DudeDao(val jdbi: Jdbi) {
    fun createTable() {
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
    }

    fun recreate() {
        jdbi.withHandle<Int, Exception> {
            it.execute(
                """
                DROP TABLE IF EXISTS Dudes;
                """.trimIndent()
            )
        }
        createTable()
    }

    fun insertDude(dude: Individual, name: String, generation: Int) {
        val serialise = serialise(dude)
        println(serialise)

        jdbi.withHandle<Int, Exception> {
            it.createUpdate(
                """
                    INSERT INTO Dudes (name, generation, individual)
                    VALUES(:name, :generation, cast (:individual as JSONB))
                """.trimIndent()
            )
                .bind("name", name)
                .bind("generation", generation)
                .bind("individual", serialise)
                .execute();
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
                    ?.deserialiseIndividual()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun listDudeSummaries(): List<DudeSummary> {
        return try {
            jdbi.withHandle<List<DudeSummary>, Exception> {
                it.createQuery(
                    """
                        SELECT name, count(*) as numGenerations FROM dudes
                        GROUP BY name
                    """.trimIndent()
                )
                    .mapToBean(DudeSummary::class.java)
                    .list()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

data class Dude(
    var id: Int,
    var individual: String?
) {
    constructor() : this(0, null)
}

// select generation, individual->'fitness' as fitness, to_timestamp(CAST(individual->'createdTimestamp' AS BIGINT) / 1000) as timestamp
// from dudes
// where name='stour';