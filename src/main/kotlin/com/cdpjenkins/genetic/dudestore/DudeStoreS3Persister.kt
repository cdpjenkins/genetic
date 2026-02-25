package com.cdpjenkins.genetic.dudestore

import com.cdpjenkins.genetic.json.fromStream
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.model.Individual
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class DudeStoreS3Persister(
    private val s3Client: S3Client,
    private val bucketName: String
) {
    fun saveToS3(individual: Individual, name: String) {
        val key = "$name/json/dude_${String.format("%010d", individual.generation)}.json"
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build(),
            RequestBody.fromString(serialise(individual))
        )
    }

    fun readFromS3(name: String, generation: Int): Individual? {
        val key = "$name/json/dude_${String.format("%010d", generation)}.json"
        return try {
            val stream = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
            )
            fromStream(stream)
        } catch (e: NoSuchKeyException) {
            null
        }
    }
}
