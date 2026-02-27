package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.dudestore.client.DudeStoreClient
import com.cdpjenkins.genetic.evolver.EvolverSettings
import com.cdpjenkins.genetic.json.fromStream
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import com.cdpjenkins.genetic.model.shape.Circle
import com.cdpjenkins.genetic.model.shape.Colour
import com.cdpjenkins.genetic.model.shape.Point
import io.kotest.matchers.comparables.shouldBeBetween
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream
import java.util.Locale
import javax.imageio.ImageIO

private const val DB_USER = "test_docker_postgres_user"
private const val DB_PASSWORD = "test_docker_postgres_password"
private const val BUCKET_NAME = "com-cdpjenkins-genetic-assets"

class WebMainIT {
    companion object {

        @JvmField
        @ClassRule
        val postgreSQLContainer = MyPostgreSQLContainer("postgres")
            .withDatabaseName("dude_db")
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD)

        val localStackContainer: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack"))
            .withServices(LocalStackContainer.Service.S3)

        lateinit var server: Http4kServer
        lateinit var dao: DudeDao
        lateinit var s3Client: S3Client
        lateinit var s3Persister: DudeStoreS3Persister

        @JvmStatic
        @BeforeAll
        fun setup() {
            postgreSQLContainer.start()
            localStackContainer.start()

            dao = DudeDao(
                Jdbi.create(
                    postgreSQLContainer.jdbcUrl,
                    DB_USER,
                    DB_PASSWORD
                )
            )

            s3Client = S3Client.builder()
                .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .region(Region.of(localStackContainer.region))
                .build()

            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build())

            s3Persister = DudeStoreS3Persister(s3Client, BUCKET_NAME)

            server = DudeStoreApplication(dao, 9000, "theCorrectSecret", s3Persister)
                .startServer()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            server.stop()
            postgreSQLContainer.stop()
            localStackContainer.stop()
        }
    }

    val secret = System.getProperty("secret", "theCorrectSecret")

    val individualLens = Body.auto<Individual>().toLens()
    val individualSummaryLens = Body.auto<IndividualSummary>().toLens()
    val dudeSummaryListLens = Body.auto<DudeSummaryList>().toLens()

    val baseUrl = "http://localhost:9000"

    val dudeStoreClient = DudeStoreClient(baseUrl, secret)
    private val client = OkHttp()

    @BeforeEach
    internal fun recreateDatabase() {
        postRecreate()
    }

    @Test
    fun `can post and retrieve Individual as JSON`() {
        postDudeAndAssertSuccess("steve", individualSteve)
        getDudeAndAssertSuccessAndDeserialise("steve") shouldBe individualSteve
    }

    @Test
    fun `multiple named individuals can be posted and retrieved`() {
        postDudeAndAssertSuccess("steve", individualSteve)
        postDudeAndAssertSuccess("brian", individualBrian)

        getDudeAndAssertSuccessAndDeserialise("steve") shouldBe individualSteve
        getDudeAndAssertSuccessAndDeserialise("brian") shouldBe individualBrian
    }

    @Test
    fun `POST dude should fail with HTTP 409 if same name and generation already exists`() {
        val individual = individualSteve.copy(generation = 1)
        postDudeAndAssertSuccess("steve", individual)
        val status = dudeStoreClient.postDude(individual, "steve")
        status shouldBe Status.CONFLICT
    }

    @Test
    fun `POST dude blows up without correct secret credentials`() {
        val status = postDudeUsingSecret("theWrongSecret")
        status shouldBe Status.UNAUTHORIZED
    }

    @Test
    fun `POST recreate blows up without correct secret credentials`() {
        val postResponse = client(
            Request(Method.POST, "${baseUrl}/recreate?secret=theWrongSecret"))
        postResponse.status shouldBe Status.UNAUTHORIZED
    }

    @Test
    fun `DB columns are populated after POST`() {
        val individual = individualWithFields(generation = 42, fitness = 99999, timeInMillis = 5678)
        postDudeAndAssertSuccess("testDude", individual)

        val dude = dao.latestDude("testDude")!!
        dude.fitness shouldBe individual.fitness
        dude.timeInMillis shouldBe individual.timeInMillis
        dude.genomeSize shouldBe individual.genome.size
        dude.createdTimestamp shouldBe individual.createdTimestamp
    }

    @Test
    fun `summary endpoint returns a summary of the latest individual`() {
        val beforePost = System.currentTimeMillis()
        postDudeAndAssertSuccess(
            "steveCopy", individualWithFields(
                generation = 123,
                fitness = 123456789,
                timeInMillis = 1234
            )
        )
        val afterPost = System.currentTimeMillis()

        val getResponse = client(
            Request(
                Method.GET,
                "${baseUrl}/dudes/${"steveCopy"}/latest/summary"
            )
        )
        getResponse.status shouldBe Status.OK
        val summary = individualSummaryLens(getResponse)
        summary.generation shouldBe 123
        summary.fitness shouldBe 123456789
        summary.timeInMillis shouldBe 1234
        summary.genomeSize shouldBe 1
        summary.timestamp.shouldBeBetween(beforePost, afterPost)
    }

    @Test
    fun `summary endpoint returns the uuid of the latest individual`() {
        postDudeAndAssertSuccess("steveCopy", individualSteve)

        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/steveCopy/latest/summary"))
        getResponse.status shouldBe Status.OK
        val summary = individualSummaryLens(getResponse)
        summary.uuid shouldBe individualSteve.uuid
    }

    @Test
    fun `summary falls back to JSONB individual when DB columns are null`() {
        val individual = individualWithFields(generation = 7, fitness = 77777, timeInMillis = 777)

        dao.jdbi.withHandle<Int, Exception> {
            it.createUpdate(
                """
                    INSERT INTO Dudes (name, generation, individual)
                    VALUES (:name, :generation, cast(:individual as JSONB))
                """.trimIndent()
            )
                .bind("name", "legacyDude")
                .bind("generation", individual.generation)
                .bind("individual", serialise(individual))
                .execute()
        }

        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/legacyDude/latest/summary"))
        getResponse.status shouldBe Status.OK
        val summary = individualSummaryLens(getResponse)
        summary.generation shouldBe 7
        summary.fitness shouldBe 77777
        summary.timeInMillis shouldBe 777
        summary.genomeSize shouldBe individual.genome.size
    }

    @Test
    fun `summary endpoint returns 404 if no dude found`() {
        val getResponse = client(
            Request(
                Method.GET,
                "${baseUrl}/dudes/doesNotExist/latest/summary"
            )
        )
        getResponse.status shouldBe Status.NOT_FOUND
    }

    @Test
    fun `list dudes endpoint returns a list of dude summaries`() {
        postDudeAndAssertSuccess("steve", individualSteve.copy(generation = 1))
        postDudeAndAssertSuccess("steve", individualSteve.copy(generation = 2))

        postDudeAndAssertSuccess("brian", individualBrian.copy(generation = 1))

        val getResponse = client(
            Request(
                Method.GET,
                "${baseUrl}/dudes"
            )
        )

        getResponse.status shouldBe Status.OK

        dudeSummaryListLens(getResponse) shouldBe
                DudeSummaryList(
                    listOf(
                        DudeSummary("brian", 1),
                        DudeSummary("steve", 2)
                    )
                )
    }

    @Test
    fun `can retrieve Individual as PNG image`() {
        postDudeAndAssertSuccess("steve", individualSteve)

        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/steve/latest?type=png"))

        getResponse.status shouldBe (Status.OK)
        getResponse.header("Content-Type") shouldBe "image/png"

        getResponse containsValidPngWithSameDimensionsAs individualSteve
    }

    @Test
    fun `returns 404 when requesting PNG for non-existent dude`() {
        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/nonexistent/latest?type=png"))
        getResponse.status shouldBe Status.NOT_FOUND
    }

    @Test
    fun `can POST and GET evolver settings`() {
        val postStatus = dudeStoreClient.postSettings("steve", EVOLVER_SETTINGS)
        postStatus shouldBe Status.OK

        dudeStoreClient.getSettings("steve") shouldBe EVOLVER_SETTINGS
    }

    @Test
    fun `POST settings should fail with HTTP 409 if the settings already exist`() {
        val postStatus = dudeStoreClient.postSettings("steve", EVOLVER_SETTINGS)
        postStatus shouldBe Status.OK
        val postStatus2 = dudeStoreClient.postSettings("steve", EVOLVER_SETTINGS)
        postStatus2 shouldBe Status.CONFLICT
    }

    @Test
    fun `can PUT to modify settings`() {
        val postStatus = dudeStoreClient.postSettings("steve", EVOLVER_SETTINGS)
        postStatus shouldBe Status.OK

        val putStatus = dudeStoreClient.putSettings("steve", EVOLVER_SETTINGS2)
        putStatus shouldBe Status.OK

        val returnedSettings = dudeStoreClient.getSettings("steve")
        returnedSettings shouldBe EVOLVER_SETTINGS2
    }

    @Test
    fun `can POST and GET master image`() {
        val byteArray = "not actual JPEG image".toByteArray()

        dudeStoreClient.postMasterImage("steve", byteArray) shouldBe Status.OK
        dudeStoreClient.getMasterImage("steve") shouldBe byteArray
    }

    @Test
    fun `can retrieve Individual by specific generation`() {
        postDudeAndAssertSuccess("steve", individualSteve.copy(generation = 1))
        postDudeAndAssertSuccess("steve", individualSteve.copy(generation = 2))
        postDudeAndAssertSuccess("steve", individualSteve.copy(generation = 3))

        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/steve/1?type=json"))
        getResponse.status shouldBe Status.OK
        val retrievedIndividual = individualLens(getResponse)
        retrievedIndividual.generation shouldBe 1
    }

    @Test
    fun `posts dude JSON to S3 when a dude is posted`() {
        postDudeAndAssertSuccess("steve", individualSteve)

        s3ShouldContainCompressed(individualSteve)
    }

    @Test
    fun `readFromS3 returns the individual that was posted`() {
        postDudeAndAssertSuccess("steve", individualSteve)

        s3Persister.readFromS3("steve", individualSteve.generation) shouldBe individualSteve
    }

    @Test
    fun `readFromS3 returns null for a name and generation that was never posted`() {
        s3Persister.readFromS3("nonexistent", 999) shouldBe null
    }

    @Test
    fun `retrieved Individual has same uuid as the one that was posted`() {
        postDudeAndAssertSuccess("steve", individualSteve)

        val retrieved = getDudeAndAssertSuccessAndDeserialise("steve")

        retrieved.uuid shouldBe individualSteve.uuid
    }

    @Test
    fun `mutating an Individual results in a different uuid`() {
        val mutated = individualSteve.mutate(EVOLVER_SETTINGS)

        mutated.uuid shouldNotBe individualSteve.uuid
    }

    @Test
    fun `uuid DB column is populated after POST`() {
        postDudeAndAssertSuccess("steve", individualSteve)

        val dudeRow = dao.jdbi.withHandle<DudeRow, Exception> {
            it.createQuery("SELECT uuid FROM Dudes WHERE name=:name")
                .bind("name", "steve")
                .mapToBean(DudeRow::class.java)
                .findOne()
                .orElse(null)
        }

        dudeRow?.uuid shouldBe individualSteve.uuid.toString()
    }

    private fun s3ShouldContainCompressed(individual: Individual) {
        val key = "steve/json/dude_${String.format(Locale.ROOT, "%010d", individual.generation)}.json.bz2"
        val responseBytes = s3Client.getObject(
            GetObjectRequest.builder().bucket(BUCKET_NAME).key(key).build()
        ).use { it.readAllBytes() }
        val retrieved = BZip2CompressorInputStream(responseBytes.inputStream()).use { fromStream(it) }
        retrieved shouldBe individual
    }

    private infix fun Response.containsValidPngWithSameDimensionsAs(individualSteve: Individual) {
        val imageBytes = body.payload.array()
        imageBytes.size shouldBeGreaterThan 0

        val bufferedImage = ImageIO.read(ByteArrayInputStream(imageBytes))
        bufferedImage.shouldNotBeNull()
        bufferedImage.width shouldBe individualSteve.bounds.width
        bufferedImage.height  shouldBe individualSteve.bounds.height
    }

    @Suppress("SameParameterValue")
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
        status shouldBe Status.OK
    }

    @Suppress("SameParameterValue")
    private fun postDudeUsingSecret(secret: String): Status =
        DudeStoreClient(baseUrl, secret)
            .postDude(individualSteve, "steve")

    private fun postRecreate() {
        val postResponse = client(
            Request(Method.POST, "${baseUrl}/recreate?secret=$secret"))
        postResponse.status shouldBe Status.OK
    }

    private fun getDudeAndAssertSuccessAndDeserialise(name: String): Individual {
        val getResponse = client(Request(Method.GET, "${baseUrl}/dudes/$name/latest?type=json"))
        getResponse.status shouldBe Status.OK
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

val EVOLVER_SETTINGS = EvolverSettings(
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

val EVOLVER_SETTINGS2 = EvolverSettings(
    "steve",
    0,
    15,
    20,
    128,
    1,
    1,
    0.5,
    0.9,
    version = 2
)
