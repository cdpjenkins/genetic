package com.cdpjenkins.genetic

import MASTER_IMAGE_FILE
import com.cdpjenkins.genetic.dudestore.client.DudeStoreClient
import com.cdpjenkins.genetic.json.deserialiseFromFile
import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.persistence.S3Client
import com.cdpjenkins.genetic.ui.GUI
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO


fun main(args: Array<String>) {
    val secret = System.getenv("SECRET")
    val name = args[0]

    val dudeClient = DudeStoreClient("https://genetic-dude.herokuapp.com", name, secret)
    val s3Client = S3Client(name)

    val initialIndividual = dudeClient.getLatestDude()

    val masterImage = ImageIO.read(File(MASTER_IMAGE_FILE))
    val evolver = makeEvolver(masterImage, initialIndividual)

    if (!GraphicsEnvironment.isHeadless()) {
        val gui = GUI(masterImage, evolver)
        gui.isVisible = true
    }

//    evolver.addListener { it.saveToDisk(name) }
    evolver.addListener { dudeClient.postDude(it) }
    evolver.addListener { s3Client.saveToS3(it); }

    evolver.start()
}
