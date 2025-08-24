package com.cdpjenkins.genetic.dudestore

import EvolverSettings
import com.cdpjenkins.genetic.dudestore.client.BlockingDudeStoreClient
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import com.cdpjenkins.genetic.model.shape.Circle
import com.cdpjenkins.genetic.model.shape.Colour
import com.cdpjenkins.genetic.model.shape.Point
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.server.Http4kServer
import org.jdbi.v3.core.Jdbi
import org.junit.ClassRule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

private const val DB_USER = "test_docker_postgres_user"
private const val DB_PASSWORD = "test_docker_postgres_password"

class WebMainIT {
    companion object {

        @JvmField
        @ClassRule
        val postgreSQLContainer = MyPostgreSQLContainer("postgres")
            .withDatabaseName("dude_db")
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD)

        lateinit var server: Http4kServer

        @JvmStatic
        @BeforeAll
        fun setup() {
            postgreSQLContainer.start()

            val dao = DudeDao(
                Jdbi.create(
                    postgreSQLContainer.jdbcUrl,
                    DB_USER,
                    DB_PASSWORD
                )
            )

            server = DudeStoreApplication(dao, 9000, "theCorrectSecret")
                .startServer()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            server.stop()

            postgreSQLContainer.stop()
        }
    }

    val secret = System.getProperty("secret", "theCorrectSecret")

    val individualLens = Body.auto<Individual>().toLens()
    val individualSummaryLens = Body.auto<IndividualSummary>().toLens()
    val dudeSummaryListLens = Body.auto<DudeSummaryList>().toLens()
    val evolverSettingsLens = Body.auto<EvolverSettings>().toLens()

    val baseUrl = "http://localhost:9000"

    val dudeStoreClient = BlockingDudeStoreClient(baseUrl, secret)
    private val client = OkHttp()

    @BeforeEach
    internal fun recreateDatabase() {
        postRecreate()
    }

    @Test
    fun `can post and retrieve Individual as JSON`() {
        postDudeAndAssertSuccess("steve", individualSteve)
        assertThat(getDudeAndAssertSuccessAndDeserialise("steve"), equalTo(individualSteve))
    }

    @Test
    fun `multiple named individuals can be posted and retrieved`() {
        postDudeAndAssertSuccess("steve", individualSteve)
        postDudeAndAssertSuccess("brian", individualBrian)

        assertThat(getDudeAndAssertSuccessAndDeserialise("steve"), equalTo(individualSteve))
        assertThat(getDudeAndAssertSuccessAndDeserialise("brian"), equalTo(individualBrian))
    }

    @Test
    fun `POST dude blows up without correct secret credentials`() {
        val status = postDudeUsingSecret("theWrongSecret")
        assertThat(status, equalTo(Status.UNAUTHORIZED))
    }

    @Test
    fun `POST recreate blows up without correct secret credentials`() {
        val postResponse = client(
            Request(Method.POST, "${baseUrl}/recreate?secret=theWrongSecret"))
        assertThat(postResponse.status, equalTo(Status.UNAUTHORIZED))
    }

    @Test
    fun `summary endpoint returns a summary of the latest individual`() {
        postDudeAndAssertSuccess(
            "steveCopy", individualWithFields(
                generation = 123,
                fitness = 123456789,
                timeInMillis = 1234
            )
        )

        val getResponse = client(
            Request(
                Method.GET,
                "${baseUrl}/dudes/${"steveCopy"}/latest/summary"
            )
        )
        assertThat(getResponse.status, equalTo(Status.OK))
        assertThat(
            individualSummaryLens(getResponse), equalTo(
                IndividualSummary(
                    generation = 123,
                    fitness = 123456789,
                    timeInMillis = 1234,
                    genomeSize = 1
                )
            )
        )
    }

    @Test
    fun `summary endpoint returns 404 if no dude found`() {
        val getResponse = client(
            Request(
                Method.GET,
                "${baseUrl}/dudes/doesNotExist/latest/summary"
            )
        )
        assertThat(getResponse.status, equalTo(Status.NOT_FOUND))
    }

    @Test
    fun `list dudes enpoint returns a list of dude summaries`() {
        postDudeAndAssertSuccess("steve", individualSteve.copy(generation = 1))
        postDudeAndAssertSuccess("steve", individualSteve.copy(generation = 2))

        postDudeAndAssertSuccess("brian", individualBrian.copy(generation = 1))

        val getResponse = client(
            Request(
                Method.GET,
                "${baseUrl}/dudes"
            )
        )

        assertThat(getResponse.status, equalTo(Status.OK))
        assertThat(
            dudeSummaryListLens(getResponse), equalTo(
                DudeSummaryList(
                    listOf(
                        DudeSummary("brian", 1),
                        DudeSummary("steve", 2)
                    )
                )
            )
        )
    }

    @Test
    fun `can retrieve Individual as PNG image`() {
        postDudeAndAssertSuccess("steve", individualSteve)

        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/steve/latest?type=png"))

        assertThat(getResponse.status, equalTo(Status.OK))
        assertThat(getResponse.header("Content-Type"), equalTo("image/png"))

        containsValidPngWithSameDimensionsAs(getResponse, individualSteve)
    }

    @Test
    fun `returns 404 when requesting PNG for non-existent dude`() {
        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/nonexistent/latest?type=png"))
        assertThat(getResponse.status, equalTo(Status.NOT_FOUND))
    }

    @Test
    fun `can POST and GET evolver settings`() {
        val evolverSettings = EvolverSettings(
            "steve",
            0,
            10,
            15,
            64,
            1,
            1,
            0.5,
            0.9
        )

        val postResponse = client(
            Request(Method.POST, "${baseUrl}/dudes/steve/settings?secret=$secret")
                .body(serialise(evolverSettings))
        )
        assertThat(postResponse.status, equalTo(Status.OK))

        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/steve/settings?type=json"))
        assertThat(getResponse.status, equalTo(Status.OK))
        val returnedSettings = evolverSettingsLens(getResponse)

        assertThat(returnedSettings, equalTo(evolverSettings))
    }

    private fun containsValidPngWithSameDimensionsAs(getResponse: Response, individualSteve: Individual) {
        val imageBytes = getResponse.body.payload.array()
        assertThat(imageBytes.size > 0, equalTo(true))

        val bufferedImage = ImageIO.read(ByteArrayInputStream(imageBytes))
        assertThat(bufferedImage, present())
        assertThat(bufferedImage.width, equalTo(individualSteve.bounds.width))
        assertThat(bufferedImage.height, equalTo(individualSteve.bounds.height))
    }

    private fun individualWithFields(
        generation: Int,
        fitness: Int,
        timeInMillis: Long
    ): Individual {
        return individualSteve.copy(generation = generation).also {
            it.fitness = fitness
            it.timeInMillis = timeInMillis
        }
    }

    private fun postDudeAndAssertSuccess(name: String, dude: Individual) {
        val status = dudeStoreClient.postDude(dude, name)
        assertThat(status, equalTo(Status.OK))
    }

    private fun postDudeUsingSecret(secret: String): Status =
        BlockingDudeStoreClient(baseUrl, secret)
            .postDude(individualSteve, "steve")

    private fun postRecreate() {
        val postResponse = client(
            Request(Method.POST, "${baseUrl}/recreate?secret=$secret"))
        assertThat(postResponse.status, equalTo(Status.OK))
    }

    private fun getDudeAndAssertSuccessAndDeserialise(name: String): Individual {
        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/$name/latest?type=json"))
        assertThat(getResponse.status, equalTo(Status.OK))
        return individualLens(getResponse)
    }

    val individualSteve = Individual(
        bounds = BoundsRectangle(0, 0, 100, 100),
        generation = 1,
        genome = listOf(
            Circle(
                Point(50, 50),
                66,
                Colour(50, 150, 200, 128),
                BoundsRectangle(-50, -50, 250, 250)
            )
        )
    )

    val individualBrian = Individual(
        bounds = BoundsRectangle(0, 0, 1000, 1000),
        generation = 1,
        genome = listOf(
            Circle(
                Point(250, 750),
                12,
                Colour(200, 50, 100, 255),
                BoundsRectangle(0, 0, 1000, 1000)
            )
        )
    )
}

class MyPostgreSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgreSQLContainer>(imageName)
