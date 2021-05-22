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
                DROP TABLE IF EXISTS Dudes;
                """.trimIndent()
            )
            it.execute(
                """
                CREATE TABLE IF NOT EXISTS Dudes(
                    id SERIAL,
                    individual JSONB NOT NULL
                );
                """.trimIndent())
        }
    }

    fun insertDude(dude: Individual) {
        val serialise = serialise(dude)
        println(serialise)

        jdbi.withHandle<Int, Exception> {
            it.createUpdate("INSERT INTO Dudes (individual) VALUES(cast (:individual as JSONB))")
                .bind("individual", serialise)
                .execute();
        }
    }

    fun latestDude(): Individual? {
        try {
            return jdbi.withHandle<Individual, Exception> {
                it.createQuery("SELECT * FROM Dudes ORDER BY ID DESC LIMIT 1")
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
}

data class Dude(
    var id: Int,
    var individual: String?
) {
    constructor() : this(0, null)
}
