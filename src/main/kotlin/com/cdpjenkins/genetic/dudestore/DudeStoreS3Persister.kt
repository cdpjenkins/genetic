package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.json.fromStream
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale

class DudeStoreS3Persister(
    private val s3Client: S3Client,
    private val bucketName: String
) {
    fun saveToS3(individual: Individual, name: String) {
        val key = "${s3KeyBase(name, individual.generation)}.json.bz2"
        val compressed = ByteArrayOutputStream().also { baos ->
            BZip2CompressorOutputStream(baos).use { it.write(serialise(individual).toByteArray()) }
        }.toByteArray()
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucketName).key(key).build(),
            RequestBody.fromBytes(compressed)
        )
    }

    fun readFromS3(name: String, generation: Int): Individual? {
        val base = s3KeyBase(name, generation)
        return readCompressedFromS3("$base.json.bz2") ?: readUncompressedFromS3("$base.json")
    }

    private fun s3KeyBase(name: String, generation: Int) =
        "$name/json/dude_${String.format(Locale.ROOT, "%010d", generation)}"

    private fun readCompressedFromS3(key: String): Individual? =
        getS3Object(key)?.use { s3Stream ->
            BZip2CompressorInputStream(s3Stream).use { compressedStream ->
                fromStream(compressedStream)
            }
        }

    private fun readUncompressedFromS3(key: String): Individual? =
        getS3Object(key)?.use { fromStream(it) }

    private fun getS3Object(key: String): InputStream? =
        try {
            s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build())
        } catch (e: NoSuchKeyException) {
            null
        }
}
