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
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
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
        .withUsername("test_user")
        .withPassword("BUuVrC0QTe9A")

    lateinit var server: Http4kServer

    @BeforeEach
    internal fun startServer() {
        postgreSQLContainer.start()

        val dao = DudeDao(
            Jdbi
                .create(
                    postgreSQLContainer.jdbcUrl,
                    "test_user",
                    "BUuVrC0QTe9A"
                )
        )

        server = makeServer(9000, "theCorrectSecret", dao)
    }

    @AfterEach
    internal fun stopServer() {
        server.stop()
    }

    private val client = OkHttp()

    @Test
    fun `can post and retrieve Individual as JSON`() {
        val serialisedIndividual = serialise(anIndividual)
        val postResponse = client(
            Request(Method.POST, "http://localhost:9000/dude?secret=theCorrectSecret")
                .body(serialisedIndividual)
        )
        assertThat(postResponse.status, equalTo(Status.OK))

        val getResponse = client(Request(Method.GET, "http://localhost:9000/dude?type=json"))
        assertThat(getResponse.status, equalTo(Status.OK))
        assertThat(
            getResponse.body.payload.asString().deserialiseIndividual(),
            equalTo(anIndividual))
    }

    @Test
    fun `POST blows up without correct secret credentials`() {
        val postResponse = client(
            Request(Method.POST, "http://localhost:9000/dude?secret=theWrongSecert")
                .body(serialise(anIndividual))
        )
        assertThat(postResponse.status, equalTo(Status.UNAUTHORIZED))
    }
}

val anIndividual = Individual(
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

class MyPostgreSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgreSQLContainer>(imageName)
