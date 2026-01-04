package com.cdpjenkins.genetic.dudestore.persistence

import com.cdpjenkins.genetic.model.Individual

/**
 * Repository interface for persisting individuals and master images.
 * Implementations can use S3, filesystem, or other storage backends.
 */
interface PersistenceRepository {
    /**
     * Format for rendered images.
     */
    enum class ImageFormat {
        PNG, SVG, JSON
    }

    /**
     * Saves an individual to the repository.
     *
     * @param name The dude name
     * @param generation The generation number
     * @param individual The individual to save
     * @return The storage key/path where the individual was saved
     */
    fun saveIndividual(name: String, generation: Int, individual: Individual): String

    /**
     * Retrieves an individual from the repository.
     *
     * @param name The dude name
     * @param generation The generation number
     * @return The individual, or null if not found
     */
    fun getIndividual(name: String, generation: Int): Individual?

    /**
     * Saves a master image to the repository.
     *
     * @param name The dude name
     * @param imageBytes The image bytes (JPEG or PNG)
     * @return The storage key/path where the master image was saved
     */
    fun saveMasterImage(name: String, imageBytes: ByteArray): String

    /**
     * Retrieves a master image from the repository.
     *
     * @param name The dude name
     * @return The image bytes, or null if not found
     */
    fun getMasterImage(name: String): ByteArray?

    /**
     * Saves a rendered image (PNG or SVG) to the repository.
     *
     * @param name The dude name
     * @param generation The generation number
     * @param format The image format (PNG or SVG)
     * @param bytes The image bytes or SVG string bytes
     * @return The storage key/path where the rendered image was saved
     */
    fun saveRenderedImage(name: String, generation: Int, format: ImageFormat, bytes: ByteArray): String

    /**
     * Retrieves a rendered image from the repository.
     *
     * @param name The dude name
     * @param generation The generation number
     * @param format The image format (PNG or SVG)
     * @return The image bytes, or null if not found
     */
    fun getRenderedImage(name: String, generation: Int, format: ImageFormat): ByteArray?
}
