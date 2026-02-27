package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import com.cdpjenkins.genetic.model.shape.Circle
import com.cdpjenkins.genetic.model.shape.Colour
import com.cdpjenkins.genetic.model.shape.Point
import com.cdpjenkins.genetic.evolver.EvolverSettings
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jdbi.v3.core.Jdbi
import org.junit.ClassRule
import org.junit.jupiter.api.*
import java.util.*

class PostgresDudeDaoIT {
    companion object {
        private const val DB_USER = "test_user"
        private const val DB_PASSWORD = "test_password"

        @JvmField
        @ClassRule
        val postgreSQLContainer = MyPostgreSQLContainer("postgres")
            .withDatabaseName("dude_db")
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD)

        lateinit var dao: DudeDao

        val individualSteve = Individual(
            bounds = BoundsRectangle(0, 0, 100, 100),
            generation = 1,
            genome = listOf(
                Circle(
                    Point(50, 50),
                    25,
                    Colour(128, 64, 32, 200),
                    BoundsRectangle(0, 0, 100, 100)
                )
            )
        )

        val individualBob = Individual(
            bounds = BoundsRectangle(0, 0, 200, 200),
            generation = 5,
            genome = listOf(
                Circle(
                    Point(100, 100),
                    50,
                    Colour(255, 128, 64, 255),
                    BoundsRectangle(0, 0, 200, 200)
                )
            )
        )

        val evolverSettings = EvolverSettings(
            "steve",
            0,
            10,
            15,
            64,
            1,
            1,
            0.5,
            0.9,
            version = 1
        )

        @JvmStatic
        @BeforeAll
        fun setup() {
            postgreSQLContainer.start()

            dao = DudeDao(
                Jdbi.create(
                    postgreSQLContainer.jdbcUrl,
                    DB_USER,
                    DB_PASSWORD
                )
            )

            dao.createTables()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            postgreSQLContainer.stop()
        }
    }

    @BeforeEach
    fun resetDatabase() {
        dao.recreate()
        dao.jdbi.withHandle<Int, Exception> {
            it.execute("DELETE FROM MasterImages")
        }
    }

    @Test
    fun `insertDude and latestDude returns the inserted dude`() {
        dao.insertDude(individualSteve, "steve", 1)

        val result = dao.latestDude("steve")

        result shouldNotBe null
        result!!.name shouldBe "steve"
        result.generation shouldBe 1
        result.fitness shouldBe individualSteve.fitness
    }

    @Test
    fun `latestDude returns null for non-existent dude`() {
        val result = dao.latestDude("nonexistent")

        result shouldBe null
    }

    @Test
    fun `latestDude returns highest generation when multiple exist`() {
        dao.insertDude(individualSteve, "steve", 1)
        dao.insertDude(individualBob, "steve", 5)

        val result = dao.latestDude("steve")

        result shouldNotBe null
        result!!.generation shouldBe 5
        result.fitness shouldBe individualBob.fitness
    }

    @Test
    fun `dudeByGeneration retrieves specific generation`() {
        dao.insertDude(individualSteve, "steve", 1)
        dao.insertDude(individualBob, "steve", 5)

        val result = dao.dudeByGeneration("steve", 1)

        result shouldNotBe null
        result!!.generation shouldBe 1
        result.name shouldBe "steve"
    }

    @Test
    fun `dudeByGeneration returns null for non-existent generation`() {
        dao.insertDude(individualSteve, "steve", 1)

        val result = dao.dudeByGeneration("steve", 99)

        result shouldBe null
    }

    @Test
    fun `insertDude populates DB columns correctly`() {
        val uuid = UUID.randomUUID()
        val individual = individualSteve.copy(uuid = uuid)

        dao.insertDude(individual, "steve", 1)

        val result = dao.latestDude("steve")

        result shouldNotBe null
        result!!.fitness shouldBe individual.fitness
        result.timeInMillis shouldBe individual.timeInMillis
        result.genomeSize shouldBe individual.genome.size
        result.createdTimestamp shouldBe individual.createdTimestamp
        result.uuid shouldBe uuid
    }

    @Test
    fun `insertDude throws AlreadyExistsException for duplicate name and generation`() {
        dao.insertDude(individualSteve, "steve", 1)

        val exception = assertThrows<AlreadyExistsException> {
            dao.insertDude(individualBob, "steve", 1)
        }

        exception.message shouldBe "Dude with name 'steve' and generation 1 already exists"
    }

    @Test
    fun `listDudeSummaries returns empty list when no dudes`() {
        val result = dao.listDudeSummaries()

        result.dudes shouldBe emptyList()
    }

    @Test
    fun `listDudeSummaries returns correct summary for single dude`() {
        dao.insertDude(individualSteve, "steve", 1)
        dao.insertDude(individualBob, "steve", 5)

        val result = dao.listDudeSummaries()

        result.dudes.size shouldBe 1
        result.dudes[0].name shouldBe "steve"
        result.dudes[0].numGenerations shouldBe 2
    }

    @Test
    fun `listDudeSummaries groups multiple dudes by name with correct generation counts`() {
        dao.insertDude(individualSteve, "steve", 1)
        dao.insertDude(individualBob, "steve", 5)
        dao.insertDude(individualSteve, "bob", 1)

        val result = dao.listDudeSummaries()

        result.dudes.size shouldBe 2
        val steveSummary = result.dudes.find { it.name == "steve" }
        val bobSummary = result.dudes.find { it.name == "bob" }

        steveSummary shouldNotBe null
        steveSummary!!.numGenerations shouldBe 2

        bobSummary shouldNotBe null
        bobSummary!!.numGenerations shouldBe 1
    }

    @Test
    fun `insertEvolverSettings and getEvolverSettings round-trip correctly`() {
        dao.insertEvolverSettings(evolverSettings)

        val result = dao.getEvolverSettings("steve")

        result shouldNotBe null
        result shouldBe evolverSettings
    }

    @Test
    fun `getEvolverSettings returns null for non-existent settings`() {
        val result = dao.getEvolverSettings("nonexistent")

        result shouldBe null
    }

    @Test
    fun `insertEvolverSettings throws AlreadyExistsException for duplicate name`() {
        dao.insertEvolverSettings(evolverSettings)

        val exception = assertThrows<AlreadyExistsException> {
            dao.insertEvolverSettings(evolverSettings)
        }

        exception.message shouldBe "Settings with name 'steve' already exists"
    }

    @Test
    fun `updateEvolverSettings persists changes correctly`() {
        dao.insertEvolverSettings(evolverSettings)

        val updatedSettings = evolverSettings.copy(maxGenomeSize = 99)
        dao.updateEvolverSettings(updatedSettings)

        val result = dao.getEvolverSettings("steve")

        result shouldNotBe null
        result!!.maxGenomeSize shouldBe 99
    }

    @Test
    fun `insertMasterImage and getMasterImage round-trip binary data`() {
        val imageData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

        dao.insertMasterImage("steve", imageData)

        val result = dao.getMasterImage("steve")

        result shouldNotBe null
        result shouldBe imageData
    }

    @Test
    fun `getMasterImage returns null for non-existent image`() {
        val result = dao.getMasterImage("nonexistent")

        result shouldBe null
    }

    @Test
    fun `insertMasterImage throws AlreadyExistsException for duplicate name`() {
        val imageData = byteArrayOf(1, 2, 3)
        dao.insertMasterImage("steve", imageData)

        val exception = assertThrows<AlreadyExistsException> {
            dao.insertMasterImage("steve", imageData)
        }

        exception.message shouldBe "MasterImage with name 'steve' already exists"
    }

    @Test
    fun `createTables creates all required tables`() {
        dao.createTables()

        dao.insertDude(individualSteve, "steve", 1)
        dao.insertEvolverSettings(evolverSettings)
        dao.insertMasterImage("steve", byteArrayOf(1, 2, 3))

        dao.latestDude("steve") shouldNotBe null
        dao.getEvolverSettings("steve") shouldNotBe null
        dao.getMasterImage("steve") shouldNotBe null
    }

    @Test
    fun `recreate clears existing data`() {
        dao.insertDude(individualSteve, "steve", 1)
        dao.insertEvolverSettings(evolverSettings)

        dao.recreate()

        dao.latestDude("steve") shouldBe null
        dao.getEvolverSettings("steve") shouldBe null
    }
}
