package com.cdpjenkins.genetic.dudestore

import software.amazon.awssdk.regions.Region
import java.net.URI

/**
 * Configuration for S3 persistence backend.
 * Reads AWS credentials and bucket information from environment variables.
 */
data class S3Config(
    val region: Region,
    val bucket: String,
    val endpointOverride: URI? = null  // For LocalStack testing
) {
    companion object {
        /**
         * Creates S3Config from environment variables.
         *
         * Required environment variables:
         * - AWS_REGION: AWS region (e.g., "eu-west-1")
         * - AWS_S3_BUCKET: S3 bucket name
         *
         * Optional environment variables:
         * - S3_ENDPOINT_OVERRIDE: Custom S3 endpoint URL (for LocalStack testing)
         */
        fun fromEnvironment(environment: Map<String, String>): S3Config {
            val regionString = environment["AWS_REGION"]
                ?: throw IllegalStateException("AWS_REGION environment variable not set")
            val bucket = environment["AWS_S3_BUCKET"]
                ?: throw IllegalStateException("AWS_S3_BUCKET environment variable not set")
            val endpointOverride = environment["S3_ENDPOINT_OVERRIDE"]?.let { URI.create(it) }

            return S3Config(
                region = Region.of(regionString),
                bucket = bucket,
                endpointOverride = endpointOverride
            )
        }

        /**
         * Creates S3Config with default production values.
         * Uses eu-west-1 region and cdpjenkins-bovine-assets bucket.
         */
        fun defaultConfig(): S3Config = S3Config(
            region = Region.EU_WEST_1,
            bucket = "cdpjenkins-bovine-assets"
        )
    }
}
