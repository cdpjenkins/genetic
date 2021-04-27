import com.cdpjenkins.genetic.model.makeEvolver
import com.cdpjenkins.genetic.model.saveToDisk
import com.cdpjenkins.genetic.ui.GUI
import com.cdpjenkins.genetic.ui.ensureDirExists
import json.JSON
import java.io.File
import javax.imageio.ImageIO
import java.awt.GraphicsEnvironment


fun main(args: Array<String>) {

    val initialIndividual = if (args.size == 1) {
        JSON().deserialiseFromFile(File(args[0]))
    } else {
        null
    }

    val masterImage = ImageIO.read(File(MASTER_IMAGE_FILE))
    val evolver = makeEvolver(masterImage, initialIndividual)

    // TODO make this code not horrible. Too many concerns are current all mixed up with the GUI,
    // with the consequence that they've had to be repeated here.
    //
    // The best way to sort this out is to get rid of SwingWorker and make the thread that runs
    // this stuff a concept that is known at the level of the Evolver. This will require a bit of
    // thought to ensure that callbacks get called on an appropriate thread (e.g. any callback that
    // hits the GUI is going to have to send something off to get executed on the AWT event thread.

    if (GraphicsEnvironment.isHeadless()) {
        ensureDirExists("output")
        ensureDirExists("output/png")
        ensureDirExists("output/json")
        ensureDirExists("output/svg")
        evolver.addListener { it.saveToDisk() }
        val runnableMeDo = object : Runnable {
            override fun run() {
                while (true) {
                    evolver.mutate()
                }
            }
        }
        val threadMeDo = Thread(runnableMeDo)
        threadMeDo.start()
        threadMeDo.join()
    } else {
        val gui = GUI(masterImage, evolver)
        gui.isVisible = true
    }
}
