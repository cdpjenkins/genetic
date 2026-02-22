package com.cdpjenkins.genetic.evolver.persistence

import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.evolver.EvolverListener
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.svg.SvgRenderer
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class S3PersistenceListener(val name: String, val awsRegion: Region, val s3Bucket: String): Persister(), EvolverListener {
    fun saveToS3(individual: Individual) {
        if (individual.generation % 10 == 0) {
            val region: Region = awsRegion
            val s3: S3Client = S3Client.builder()
                .region(region)
                .build()

            val baos = ByteArrayOutputStream()
            ImageIO.write(individual.bufferedImage, "png", baos)
            s3.putObject(
                PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(pngFileName(individual, name))
                    .build(),
                RequestBody.fromBytes(baos.toByteArray())
            )

            s3.putObject(
                PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(jsonFileName(individual, name))
                    .build(),
                RequestBody.fromString(serialise(individual))
            )

            val svgString = SvgRenderer().renderToString(individual)
            val svgFile = svgFileName(individual, name)
            s3.putObject(
                PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(svgFile)
                    .build(),
                RequestBody.fromString(svgString)
            )
        }
    }

    override fun notify(individual: Individual) {
        saveToS3(individual)
    }

    companion object {
        fun create(name: String): S3PersistenceListener = S3PersistenceListener(name, Region.EU_WEST_1, "cdpjenkins-bovine-assets")
    }
}