import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics2D
import java.awt.LayoutManager
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel


class GUI(title: String? = "Genetic!") : JFrame(title) {
    constructor(): this("Genetic!") {
        println("poo")

        contentPane.add(UIPanel())
        pack()
    }

    class UIPanel(layout: LayoutManager): JPanel(layout) {
        val icon: ImageIcon  = ImageIcon(ImageIO.read(File("cow.jpg")))

        constructor(): this(BorderLayout()) {
            val bufferedImage =
                BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g = bufferedImage.createGraphics()

            val shapes = (1..100).map { spawnRandomShape(icon.iconWidth, icon.iconHeight) }

            Individual(shapes).draw(g)

            add(JLabel(icon), BorderLayout.WEST)
            add(JLabel(ImageIcon(bufferedImage)), BorderLayout.EAST)
        }
    }

}

data class Colour(val r: Int, val g:Int, val b: Int, val a:Int) {
    fun getColor(): Color = Color(r, g, b, a)
}

interface Shape {
    fun draw(g: Graphics2D)
}

data class Circle(val x: Int, val y: Int, val colour: Colour, private val radius: Int) : Shape {
    override fun draw(g: Graphics2D) {
        g.color = colour.getColor()
        g.fillOval(x - radius/2, y - radius/2, radius, radius)
    }
}

class Individual(var genome: List<Circle>) {
    fun draw(g: Graphics2D) {
        for (shape in genome) {
            shape.draw(g)
        }
    }
}

fun spawnRandomShape(width: Int, height: Int): Circle {
    val random = Random()
    return Circle(
        random.nextInt(width),
        random.nextInt(height),
        Colour(random.nextInt(255), random.nextInt(255), random.nextInt(255), random.nextInt(255)),
        random.nextInt(50)
    )
}

private fun makeGUI(): GUI {
    var gui = GUI()

    return gui
}

fun main(args: Array<String>) {
    val gui = makeGUI()
    gui.isVisible = true
}

