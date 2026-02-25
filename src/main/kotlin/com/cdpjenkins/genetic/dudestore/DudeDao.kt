package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.evolver.EvolverSettings
import com.cdpjenkins.genetic.json.deserialise
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import java.sql.SQLException

class DudeDao(val jdbi: Jdbi) {
    fun createTables() {
        jdbi.withHandle<Int, Exception> {
            it.execute(
                """
                CREATE TABLE IF NOT EXISTS Dudes(
                    name VARCHAR(50) NOT NULL,
                    generation INT NOT NULL,
                    individual JSONB,
                    fitness INTEGER,
                    timeInMillis INTEGER,
                    genomeSize INTEGER,
                    createdTimestamp BIGINT,
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

        jdbi.withHandle<Int, Exception> {
            it.execute(
                """
                CREATE TABLE IF NOT EXISTS MasterImages(
                    name VARCHAR PRIMARY KEY,
                    fileData BYTEA NOT NULL
                );
                """.trimIndent())
        }
    }

    fun recreate() {
        jdbi.withHandle<Int, Exception> {
            it.execute(
                """
                DROP TABLE IF EXISTS Dudes;
                DROP TABLE IF EXISTS EvolverSettings;
                """.trimIndent()
            )
        }
        createTables()
    }

    fun insertDude(dude: Individual, name: String, generation: Int) {
        jdbi.withHandle<Int, Exception> {
            it.createUpdate(
                """
                    INSERT INTO Dudes (name, generation)
                    VALUES(:name, :generation)
                """.trimIndent()
            )
                .bind("name", name)
                .bind("generation", generation)
                .execute()
        }
    }

    fun latestDude(name: String): Dude? {
        try {
            return jdbi.withHandle<Dude, Exception> {
                it.createQuery(
                    """
                        SELECT name, generation, individual FROM dudes
                        WHERE name=:name
                        ORDER BY generation DESC LIMIT 1
                    """.trimIndent()
                )
                    .bind("name", name)
                    .mapToBean(DudeRow::class.java)
                    .findOne()
                    .orElse(null)
                    ?.let { Dude.from(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun dudeByGeneration(name: String, generation: Int): Dude? {
        try {
            return jdbi.withHandle<Dude, Exception> {
                it.createQuery(
                    """
                        SELECT name, generation, individual FROM dudes
                        WHERE name=:name AND generation=:generation
                    """.trimIndent()
                )
                    .bind("name", name)
                    .bind("generation", generation)
                    .mapToBean(DudeRow::class.java)
                    .findOne()
                    .orElse(null)
                    ?.let { Dude.from(it) }
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
        val name = evolverSettings.name

        try {
            jdbi.withHandle<Int, Exception> {
                it.createUpdate(
                    """
                    INSERT INTO EvolverSettings (name, settings)
                    VALUES(:name, cast (:settings as JSONB))
                """.trimIndent()
                )
                    .bind("name", name)
                    .bind("settings", serialise(evolverSettings))
                    .execute()
            }
        } catch (e: UnableToExecuteStatementException) {
            if (e.cause.isPostgresUniqueConstraintViolation()) {
                throw SettingsAlreadyExistsException("Settings with name '$name' already exists", e)
            }
            throw e
        }
    }

    fun updateEvolverSettings(evolverSettings: EvolverSettings) {
        val name = evolverSettings.name

        jdbi.withHandle<Int, Exception> {
            it.createUpdate(
                """
                    UPDATE EvolverSettings
                    SET settings = cast (:settings as JSONB)
                    WHERE name = :name
                """.trimIndent()
            )
                .bind("name", name)
                .bind("settings", serialise(evolverSettings))
                .execute()
        }
    }

    private fun Throwable?.isPostgresUniqueConstraintViolation() =
        this is SQLException &&
        this.sqlState == POSTGRES_UNIQUE_VIOLATION

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

    fun insertMasterImage(name: String, masterImageBytes: ByteArray) {
        try {
            jdbi.withHandle<Int, Exception> {
                it.createUpdate(
                    """
                    INSERT INTO MasterImages (name, fileData)
                    VALUES(:name, :fileData)
                """.trimIndent()
                )
                    .bind("name", name)
                    .bind("fileData", masterImageBytes)
                    .execute()
            }
        } catch (e: UnableToExecuteStatementException) {
            if (e.cause.isPostgresUniqueConstraintViolation()) {
                throw SettingsAlreadyExistsException("MasterImage with name '$name' already exists", e)
            }
            throw e
        }
    }

    fun getMasterImage(name: String): ByteArray? {
        return jdbi.withHandle<ByteArray, Exception> {
            it.createQuery(
                """
                    SELECT fileData FROM MasterImages
                    WHERE name=:name
                """.trimIndent()
            )
                .bind("name", name)
                .mapToBean(FileDataRow::class.java)
                .findOne()
                .orElse(null)
                ?.fileData
        }
    }
}

data class DudeRow(
    var name: String,
    var generation: Int,
    var individual: String?
) {
    @Suppress("unused")
    constructor() : this("", 0, null)
}

data class Dude(
    val name: String,
    val generation: Int,
    val individual: Individual?
) {
    companion object {
        fun from(dudeRow: DudeRow): Dude {
            return Dude(dudeRow.name, dudeRow.generation, dudeRow.individual?.deserialise())
        }
    }
}

data class EvolverSettingsRow(
    var name: String,
    var settings: String?
) {
    @Suppress("unused")
    constructor() : this("", null)
}

class FileDataRow(
    var name: String,
    var fileData: ByteArray?
) {
    @Suppress("unused")
    constructor() : this("", null)
}

class SettingsAlreadyExistsException(message: String, cause: Exception) : Exception(message, cause)

private const val POSTGRES_UNIQUE_VIOLATION = "23505"
