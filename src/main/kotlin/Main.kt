import com.cdpjenkins.genetic.ui.GUI
import json.JSON
import java.io.File

fun main(args: Array<String>) {

    val gui = if (args.size == 1) {
        GUI(JSON().deserialiseFromFile(File(args[0])))
    } else {
        GUI()
    }

    gui.isVisible = true
}
