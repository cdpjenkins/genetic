

Please run the tests before making any changes.

│
│                                       
│ Context                                                                                                                                                                      │
│                                                                                                                                                                              │
│ Individual JSON files are already written to S3 as plain .json (implemented in the previous session).                                                                        │
│ The goal is to compress them with bzip2 going forward to reduce S3 storage and transfer costs.                                                                               │
│ New files must be stored as .json.bz2. The read path must fall back gracefully for old uncompressed                                                                          │
│ .json files, and for legacy rows still carrying data in the Postgres individual column.                                                                                      │
│                                                                                                                                                                              │
│ There is also a pre-existing typo in readFromS3 (line 28): generation)2.json — the closing } of                                                                              │
│ the string template was replaced with 2, causing a compile error. This must be fixed.                                                                                        │
│                                                                                                                                                                              │
│ Read path priority (after change)                                                                                                                                            │
│                                                                                                                                                                              │
│ 1. Postgres individual column non-null → use it (legacy Postgres rows)                                                                                                       │
│ 2. .json.bz2 key exists in S3 → decompress with bzip2 and return                                                                                                             │
│ 3. .json key exists in S3 → return uncompressed (legacy S3 rows)                                                                                                             │
│ 4. Neither exists → return null → HTTP 404                                                                                                                                   │
│                                                                                                                                                                              │
│ Files to change                                                                                                                                                              │
│                                                                                                                                                                              │
│ ┌─────────────────────────┬─────────────────────────────────────────────────────────────────┐                                                                                │
│ │          File           │                             Change                              │                                                                                │
│ ├─────────────────────────┼─────────────────────────────────────────────────────────────────┤                                                                                │
│ │ build.gradle            │ Add org.apache.commons:commons-compress dependency              │                                                                                │
│ ├─────────────────────────┼─────────────────────────────────────────────────────────────────┤                                                                                │
│ │ DudeStoreS3Persister.kt │ Fix typo; compress write path; add fallback read path           │                                                                                │
│ ├─────────────────────────┼─────────────────────────────────────────────────────────────────┤                                                                                │
│ │ WebMainIT.kt            │ Update s3ShouldContain to read .json.bz2; add compression tests │                                                                                │
│ └─────────────────────────┴─────────────────────────────────────────────────────────────────┘                                                                                │
│                                                                                                                                                                              │
│ WebMain.kt and DudeDao.kt require no changes — resolveIndividual already delegates to                                                                                        │
│ readFromS3, which will encapsulate the compressed/uncompressed fallback.                                                                                                     │
│                                                                                                                                                                              │
│ Dependency                                                                                                                                                                   │
│                                                                                                                                                                              │
│ Add to build.gradle (under dependencies):                                                                                                                                    │
│ implementation 'org.apache.commons:commons-compress:1.27.1'                                                                                                                  │
│                                                                                                                                                                              │
│ Increment 1 — RED: Update test to expect .json.bz2                                                                                                                           │
│                                                                                                                                                                              │
│ File: src/intTest/kotlin/com/cdpjenkins/genetic/dudestore/WebMainIT.kt                                                                                                       │
│                                                                                                                                                                              │
│ Update s3ShouldContain to read the .json.bz2 key and decompress it:                                                                                                          │
│ private fun s3ShouldContain(individual: Individual) {                                                                                                                        │
│     val key = "steve/json/dude_${String.format("%010d", individual.generation)}.json.bz2"                                                                                    │
│     val responseBytes = s3Client.getObject(                                                                                                                                  │
│         GetObjectRequest.builder().bucket(BUCKET_NAME).key(key).build()                                                                                                      │
│     ).readAllBytes()                                                                                                                                                         │
│     val retrieved = BZip2CompressorInputStream(responseBytes.inputStream()).use { fromStream(it) }                                                                           │
│     retrieved shouldBe individual                                                                                                                                            │
│ }                                                                                                                                                                            │
│                                                                                                                                                                              │
│ This makes posts dude JSON to S3 when a dude is posted RED because saveToS3 still writes .json.                                                                              │
│                                                                                                                                                                              │
│ Increment 2 — GREEN: Compress write path in saveToS3                                                                                                                         │
│                                                                                                                                                                              │
│ File: src/main/kotlin/com/cdpjenkins/genetic/dudestore/DudeStoreS3Persister.kt                                                                                               │
│                                                                                                                                                                              │
│ Also fix the typo in readFromS3 (line 28: generation)2.json → generation)}.json).                                                                                            │
│                                                                                                                                                                              │
│ Change saveToS3 to:                                                                                                                                                          │
│ fun saveToS3(individual: Individual, name: String) {                                                                                                                         │
│     val key = "$name/json/dude_${String.format("%010d", individual.generation)}.json.bz2"                                                                                    │
│     val compressed = ByteArrayOutputStream().also { baos ->                                                                                                                  │
│         BZip2CompressorOutputStream(baos).use { it.write(serialise(individual).toByteArray()) }                                                                              │
│     }.toByteArray()                                                                                                                                                          │
│     s3Client.putObject(                                                                                                                                                      │
│         PutObjectRequest.builder().bucket(bucketName).key(key).build(),                                                                                                      │
│         RequestBody.fromBytes(compressed)                                                                                                                                    │
│     )                                                                                                                                                                        │
│ }                                                                                                                                                                            │
│                                                                                                                                                                              │
│ After this, posts dude JSON to S3 goes GREEN. But readFromS3 returns the individual that was posted                                                                          │
│ and all end-to-end tests go RED — readFromS3 still looks for .json, which is no longer written.                                                                              │
│                                                                                                                                                                              │
│ Increment 3 — GREEN: Add compressed + fallback read path to readFromS3                                                                                                       │
│                                                                                                                                                                              │
│ Replace the single-key readFromS3 with a two-key fallback:                                                                                                                   │
│ fun readFromS3(name: String, generation: Int): Individual? {                                                                                                                 │
│     val base = "$name/json/dude_${String.format("%010d", generation)}"                                                                                                       │
│     return readCompressedFromS3("$base.json.bz2") ?: readUncompressedFromS3("$base.json")                                                                                    │
│ }                                                                                                                                                                            │
│                                                                                                                                                                              │
│ private fun readCompressedFromS3(key: String): Individual? =                                                                                                                 │
│     getS3Object(key)?.let { fromStream(BZip2CompressorInputStream(it)) }                                                                                                     │
│                                                                                                                                                                              │
│ private fun readUncompressedFromS3(key: String): Individual? =                                                                                                               │
│     getS3Object(key)?.let { fromStream(it) }                                                                                                                                 │
│                                                                                                                                                                              │
│ private fun getS3Object(key: String): InputStream? =                                                                                                                         │
│     try {                                                                                                                                                                    │
│         s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build())                                                                                   │
│     } catch (e: NoSuchKeyException) {                                                                                                                                        │
│         null                                                                                                                                                                 │
│     }                                                                                                                                                                        │
│                                                                                                                                                                              │
│ Imports needed: org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream,                                                                                    │
│ org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream,                                                                                                   │
│ java.io.ByteArrayOutputStream, java.io.InputStream.                                                                                                                          │
│                                                                                                                                                                              │
│ After this, all tests go GREEN.                                                                                                                                              │
│                                                                                                                                                                              │
│ Verification                                                                                                                                                                 │
│                                                                                                                                                                              │
│ Run ./gradlew intTest — all 17 existing tests must pass plus any new ones added.                                                                                             │
│ Key tests that exercise the S3 path end-to-end:                                                                                                                              │
│ - can post and retrieve Individual as JSON                                                                                                                                   │
│ - can retrieve Individual as PNG image                                                                                                                                       │
│ - summary endpoint returns a summary of the latest individual                                                                                                                │
│ - can retrieve Individual by specific generation                                                                                                                             │
│ - posts dude JSON to S3 when a dude is posted                                                                                                                                │
│ - readFromS3 returns the individual that was posted                                                                                                                          │
│ - readFromS3 returns null for a name and generation that was never posted         
