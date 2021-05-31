package com.cdpjenkins.genetic.model

import com.cdpjenkins.genetic.image.grabPixels
import com.cdpjenkins.genetic.image.writePng
import com.cdpjenkins.genetic.json.serialise
import com.cdpjenkins.genetic.json.serialiseToFile
import com.cdpjenkins.genetic.model.shape.BoundsRectangle
import com.cdpjenkins.genetic.svg.SvgRenderer
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

class Evolver(var individual: Individual, masterImage: BufferedImage) {
    private var listeners: MutableList<EvolverListener> = mutableListOf()
    private val masterPixels = grabPixels(masterImage)

    init {
        ensureDirExists("output")
        ensureDirExists("output/png")
        ensureDirExists("output/json")
        ensureDirExists("output/svg")
    }

    @Synchronized fun mutate() {
        val newIndividual = individual.mutate()
        newIndividual.drawAndCalculateFitness(masterPixels)
        if (newIndividual.fitness < individual.fitness) {
            individual = newIndividual
            println(individual.describe())

//            individual.drawDiff(masterPixels)

            listeners.forEach{ it.notify(individual) }
        }
    }

    @Synchronized fun addListener(listener: EvolverListener) {
        this.listeners.add(listener)
    }

    @Synchronized
    fun start() {
        val runnableMeDo = object : Runnable {
            override fun run() {
                while (true) {
                    mutate()
                }
            }
        }
        val threadMeDo = Thread(runnableMeDo)
        threadMeDo.start()
    }
}

fun makeEvolver(
    masterImage: BufferedImage,
    initialIndividual: Individual?
): Evolver {
    val boundsRectangle = BoundsRectangle(0, 0, masterImage.width, masterImage.height)
    val individual = initialIndividual ?: makeIndividual(masterImage, boundsRectangle)
    val evolver = Evolver(individual, masterImage)
    return evolver
}

fun Individual.saveToDisk() {
    if (generation % 10 == 0) {
        val pngFile = File(String.format("output/png/cow_%010d.png", generation))
        writePng(this, pngFile)

        val jsonFile = File(String.format("output/json/cow_%010d.json", generation))
        serialiseToFile(jsonFile, this)

        SvgRenderer().renderToFile(
            File(String.format("output/svg/cow_%010d.svg", generation)),
            this
        )
    }
}

fun Individual.saveToS3() {
    if (generation % 10 == 0) {
        val region: Region = Region.EU_WEST_1
        val s3: S3Client = S3Client.builder()
            .region(region)
            .build()

        val baos = ByteArrayOutputStream()
        ImageIO.write(this.bufferedImage, "png", baos)
        s3.putObject(
            PutObjectRequest.builder()
                .bucket("cdpjenkins-bovine-assets")
                .key(String.format("colin/png/cow_%010d.png", generation))
                .build(),
            RequestBody.fromBytes(baos.toByteArray())
        )

        s3.putObject(
            PutObjectRequest.builder()
                .bucket("cdpjenkins-bovine-assets")
                .key(String.format("colin/json/cow_%010d.json", generation))
                .build(),
            RequestBody.fromString(serialise(this))
        )

        val svgString = SvgRenderer().renderToString(this)
        val svgFile = String.format("colin/svg/cow_%010d.svg", generation)
        s3.putObject(
            PutObjectRequest.builder()
                .bucket("cdpjenkins-bovine-assets")
                .key(svgFile)
                .build(),
            RequestBody.fromString(svgString)
        )
    }
}

fun interface EvolverListener {
    fun notify(individual: Individual)
}

fun ensureDirExists(dirName: String) {
    val dir = File(dirName)
    if (!dir.exists()) dir.mkdir()
}