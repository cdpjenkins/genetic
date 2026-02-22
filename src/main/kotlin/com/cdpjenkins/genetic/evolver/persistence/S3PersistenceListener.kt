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

class S3PersistenceListener(val persister: S3Persister): EvolverListener {
    override fun notify(individual: Individual) {
        if (individual.generation % 10 == 0) {
            persister.saveToS3(individual)
        }
    }
}

class S3Persister(private val s3Client: S3Client, val name: String, val s3Bucket: String) : Persister() {
    companion object {
        fun create(name: String): S3Persister {
            val s3Client: S3Client = S3Client.builder().region(Region.EU_WEST_1).build()

            return S3Persister(s3Client, name, "cdpjenkins-bovine-assets")
        }
    }

    fun saveToS3(individual: Individual) {
        s3Client.writeJSON(individual)
        s3Client.writePNG(individual)
        s3Client.writeSVG(individual)
    }

    private fun S3Client.writeSVG(individual: Individual) {
        val svgString = SvgRenderer().renderToString(individual)
        val svgFile = svgFileName(individual, name)
        putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(svgFile)
                .build(),
            RequestBody.fromString(svgString)
        )
    }

    private fun S3Client.writeJSON(individual: Individual) {
        putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(jsonFileName(individual, name))
                .build(),
            RequestBody.fromString(serialise(individual))
        )
    }

    private fun S3Client.writePNG(individual: Individual) {
        val baos = ByteArrayOutputStream()
        ImageIO.write(individual.bufferedImage, "png", baos)
        putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(pngFileName(individual, name))
                .build(),
            RequestBody.fromBytes(baos.toByteArray())
        )
    }
}
