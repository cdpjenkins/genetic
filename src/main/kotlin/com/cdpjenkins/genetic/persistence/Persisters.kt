package com.cdpjenkins.genetic.persistence

import com.cdpjenkins.genetic.image.writePng
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.json.serialiseToFile
import com.cdpjenkins.genetic.model.Individual
import com.cdpjenkins.genetic.svg.SvgRenderer
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

fun Individual.saveToDisk(name: String) {
    if (generation % 10 == 0) {
        val pngFile = File(pngFileName(this, name))
        writePng(this, pngFile)

        val jsonFile = File(jsonFileName(this, name))
        serialiseToFile(jsonFile, this)

        SvgRenderer().renderToFile(
            File(svgFileName(this, name)),
            this
        )
    }
}

// TODO violates single responsibility principle
// separate S3 stuff from stuff that knows about file names, formats etc
// pull out bucket-na

class S3Client(val name: String) {
    fun saveToS3(individual: Individual) {
        if (individual.generation % 10 == 0) {
            val region: Region = Region.EU_WEST_1
            val s3: S3Client = S3Client.builder()
                .region(region)
                .build()

            val baos = ByteArrayOutputStream()
            ImageIO.write(individual.bufferedImage, "png", baos)
            s3.putObject(
                PutObjectRequest.builder()
                    .bucket("cdpjenkins-bovine-assets")
                    .key(pngFileName(individual, name))
                    .build(),
                RequestBody.fromBytes(baos.toByteArray())
            )

            s3.putObject(
                PutObjectRequest.builder()
                    .bucket("cdpjenkins-bovine-assets")
                    .key(jsonFileName(individual, name))
                    .build(),
                RequestBody.fromString(serialise(individual))
            )

            val svgString = SvgRenderer().renderToString(individual)
            val svgFile = svgFileName(individual, name)
            s3.putObject(
                PutObjectRequest.builder()
                    .bucket("cdpjenkins-bovine-assets")
                    .key(svgFile)
                    .build(),
                RequestBody.fromString(svgString)
            )
        }
    }
}

private fun svgFileName(individual: Individual, name: String) =
    String.format("$name/svg/cow_%010d.svg", individual.generation)

private fun jsonFileName(individual: Individual, name: String) =
    String.format("$name/json/cow_%010d.json", individual.generation)

private fun pngFileName(individual: Individual, name: String) =
    String.format("$name/png/cow_%010d.png", individual.generation)