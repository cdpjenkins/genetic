package com.cdpjenkins.genetic.dudestore.persistence

import com.cdpjenkins.genetic.dudestore.S3Config
import com.cdpjenkins.genetic.json.deserialise
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.svg.SvgRenderer
import mu.KotlinLogging
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

/**
 * S3-based implementation of PersistenceRepository.
 * Stores individuals, master images, and rendered images in AWS S3.
 */
class S3Repository(
    private val config: S3Config
) : PersistenceRepository {

    private val s3Client: S3Client = createS3Client()

    private fun createS3Client(): S3Client {
        val builder = S3Client.builder()
            .region(config.region)

        // Support LocalStack endpoint override for testing
        config.endpointOverride?.let { builder.endpointOverride(it) }

        return builder.build()
    }

    override fun saveIndividual(name: String, generation: Int, individual: Individual): String {
        val key = individualKey(name, generation)
        val jsonContent = serialise(individual)

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(config.bucket)
                .key(key)
                .contentType("application/json")
                .build(),
            RequestBody.fromString(jsonContent)
        )

        logger.info { "Saved individual to S3: s3://${config.bucket}/$key" }
        return key
    }

    override fun getIndividual(name: String, generation: Int): Individual? {
        val key = individualKey(name, generation)

        return try {
            val response = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .build()
            )

            val jsonContent = response.readAllBytes().decodeToString()
            jsonContent.deserialise<Individual>()
        } catch (e: NoSuchKeyException) {
            logger.warn { "Individual not found in S3: s3://${config.bucket}/$key" }
            null
        }
    }

    override fun saveMasterImage(name: String, imageBytes: ByteArray): String {
        val key = masterImageKey(name)

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(config.bucket)
                .key(key)
                .contentType("image/jpeg")
                .build(),
            RequestBody.fromBytes(imageBytes)
        )

        logger.info { "Saved master image to S3: s3://${config.bucket}/$key" }
        return key
    }

    override fun getMasterImage(name: String): ByteArray? {
        val key = masterImageKey(name)

        return try {
            val response = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .build()
            )
            response.readAllBytes()
        } catch (e: NoSuchKeyException) {
            logger.warn { "Master image not found in S3: s3://${config.bucket}/$key" }
            null
        }
    }

    override fun saveRenderedImage(
        name: String,
        generation: Int,
        format: PersistenceRepository.ImageFormat,
        bytes: ByteArray
    ): String {
        val key = renderedImageKey(name, generation, format)
        val contentType = when (format) {
            PersistenceRepository.ImageFormat.PNG -> "image/png"
            PersistenceRepository.ImageFormat.SVG -> "image/svg+xml"
            PersistenceRepository.ImageFormat.JSON -> "application/json"
        }

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(config.bucket)
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(bytes)
        )

        logger.debug { "Saved rendered image to S3: s3://${config.bucket}/$key" }
        return key
    }

    override fun getRenderedImage(
        name: String,
        generation: Int,
        format: PersistenceRepository.ImageFormat
    ): ByteArray? {
        val key = renderedImageKey(name, generation, format)

        return try {
            val response = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .build()
            )
            response.readAllBytes()
        } catch (e: NoSuchKeyException) {
            logger.warn { "Rendered image not found in S3: s3://${config.bucket}/$key" }
            null
        }
    }

    /**
     * Saves PNG and SVG renderings of an individual to S3.
     * Renders the individual on-demand and uploads both formats.
     *
     * @param name The dude name
     * @param individual The individual to render and save
     */
    fun saveRenderedImages(name: String, individual: Individual) {
        // Render and save PNG
        val baos = ByteArrayOutputStream()
        ImageIO.write(individual.bufferedImage, "png", baos)
        saveRenderedImage(name, individual.generation, PersistenceRepository.ImageFormat.PNG, baos.toByteArray())

        // Render and save SVG
        val svgString = SvgRenderer().renderToString(individual)
        saveRenderedImage(name, individual.generation, PersistenceRepository.ImageFormat.SVG, svgString.toByteArray())
    }

    // Key naming strategies - consistent with existing S3Persister conventions
    private fun individualKey(name: String, generation: Int): String =
        String.format("$name/json/cow_%010d.json", generation)

    private fun masterImageKey(name: String): String =
        "$name/master-image.jpg"

    private fun renderedImageKey(name: String, generation: Int, format: PersistenceRepository.ImageFormat): String =
        when (format) {
            PersistenceRepository.ImageFormat.PNG -> String.format("$name/png/cow_%010d.png", generation)
            PersistenceRepository.ImageFormat.SVG -> String.format("$name/svg/cow_%010d.svg", generation)
            PersistenceRepository.ImageFormat.JSON -> String.format("$name/json/cow_%010d.json", generation)
        }
}
