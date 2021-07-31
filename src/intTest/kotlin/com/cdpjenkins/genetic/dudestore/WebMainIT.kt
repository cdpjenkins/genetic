package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.json.deserialiseIndividual
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import com.cdpjenkins.genetic.model.shape.Circle
import com.cdpjenkins.genetic.model.shape.Colour
import com.cdpjenkins.genetic.model.shape.Point
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.asString
import org.http4k.client.OkHttp
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.server.Http4kServer
import org.jdbi.v3.core.Jdbi
import org.junit.ClassRule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer

class WebMainIT {
    @ClassRule
    var postgreSQLContainer = MyPostgreSQLContainer("postgres")
        .withDatabaseName("dude_db")
        .withUsername("test_docker_postgres_user")
        .withPassword("test_docker_postgres_password")

    val secret = System.getProperty("secret", "theCorrectSecret")

    val individualLens = Body.auto<Individual>().toLens()
    val individualSummaryLens = Body.auto<IndividualSummary>().toLens()

    lateinit var server: Http4kServer

    @BeforeEach
    internal fun startServer() {
        postgreSQLContainer.start()

        val dao = DudeDao(
            Jdbi
                .create(
                    postgreSQLContainer.jdbcUrl,
                    "test_docker_postgres_user",
                    "test_docker_postgres_password"
                )
        )

        server = makeServer(9000, "theCorrectSecret", dao)
    }

    @BeforeEach
    internal fun recreateDatabase() {
        postRecreate()
    }

    @AfterEach
    internal fun stopServer() {
        server.stop()
    }

    private val client = OkHttp()

    @Test
    fun `can post and retrieve Individual as JSON`() {
        val postResponse = client(
            Request(Method.POST, "http://localhost:9000/dude/steve?secret=$secret")
                .body(serialise(individualSteve))
        )
        assertThat(postResponse.status, equalTo(Status.OK))

        val getResponse = client(Request(Method.GET, "http://localhost:9000/dude/steve/latest?type=json"))
        assertThat(getResponse.status, equalTo(Status.OK))
        assertThat(
            getResponse.body.payload.asString().deserialiseIndividual(),
            equalTo(individualSteve))
    }

    @Test
    fun `multiple named individuals can be posted and retrieved`() {
        postDude("steve", individualSteve)
        postDude("brian", individualBrian)

        assertThat(individualLens(getDude("steve")), equalTo(individualSteve))
        assertThat(individualLens(getDude("brian")), equalTo(individualBrian))
    }

    @Test
    fun `POST dude blows up without correct secret credentials`() {
        val postResponse = client(
            Request(Method.POST, "http://localhost:9000/dude/steve?secret=theWrongSecert")
                .body(serialise(individualSteve))
        )
        assertThat(postResponse.status, equalTo(Status.UNAUTHORIZED))
    }

    @Test
    fun `POST recreate blows up without correct secret credentials`() {
        val postResponse = client(
            Request(Method.POST, "http://localhost:9000/recreate?secret=theWrongSecert"))
        assertThat(postResponse.status, equalTo(Status.UNAUTHORIZED))
    }

    @Test
    fun `summary endpoint returns a summary`() {
        postDude(
            "steveCopy", individualWithFields(
                generation = 123,
                fitness = 123456789,
                timeInMillis = 1234
            )
        )

        val getResponse = client(
            Request(
                Method.GET,
                "http://localhost:9000/dude/${"steveCopy"}/latest/summary"
            )
        )
        assertThat(getResponse.status, equalTo(Status.OK))
        assertThat(
            individualSummaryLens(getResponse), equalTo(
                IndividualSummary(
                    generation = 123,
                    fitness = 123456789,
                    timeInMillis = 1234
                )
            )
        )
    }

    @Test
    fun `summary endpoint returns 404 if no dude found`() {
        val getResponse = client(
            Request(
                Method.GET,
                "http://localhost:9000/dude/doesNotExist/latest/summary"
            )
        )
        assertThat(getResponse.status, equalTo(Status.NOT_FOUND))
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

    private fun postDude(name: String, dude: Individual) {
        val postResponse = client(
            Request(Method.POST, "http://localhost:9000/dude/$name?secret=$secret")
                .body(serialise(dude))
        )
        assertThat(postResponse.status, equalTo(Status.OK))
    }

    private fun postRecreate() {
        val postResponse = client(
            Request(Method.POST, "http://localhost:9000/recreate?secret=$secret"))
        assertThat(postResponse.status, equalTo(Status.OK))
    }

    private fun getDude(name: String): Response {
        val getResponse = client(Request(Method.GET, "http://localhost:9000/dude/$name/latest?type=json"))
        assertThat(getResponse.status, equalTo(Status.OK))
        return getResponse
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
