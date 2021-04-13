import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics2D
import java.awt.LayoutManager
import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*


class GUI(title: String? = "Genetic!") : JFrame(title) {
    constructor(): this("Genetic!") {
        println("poo")

        contentPane.add(UIPanel())
        pack()
    }

    class UIPanel(layout: LayoutManager): JPanel(layout) {
        val masterImage: BufferedImage = ImageIO.read(File("cow.jpg"))
        val masterIcon: ImageIcon  = ImageIcon(masterImage)

        constructor(): this(BorderLayout()) {
            val width = masterImage.width
            val height = masterImage.height

            val bufferedImage =
                BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g = bufferedImage.createGraphics()

            val shapes = (1..100).map { spawnRandomShape(width, height) }

            val individual = Individual(shapes, masterImage, width, height)
            individual.drawAndCalculateFitness()
            individual.draw(g)

            add(JLabel(masterIcon), BorderLayout.WEST)
            add(JLabel(ImageIcon(individual.bufferedImage)), BorderLayout.EAST)

            val mutateButton = JButton("Mutate!")
            add(mutateButton, BorderLayout.SOUTH)
            mutateButton.addActionListener {
                    e -> individual.mutate()
                    repaint()
            }
        }
    }
}

data class Colour(val r: Int, val g:Int, val b: Int, val a:Int) {
    fun getColor(): Color = Color(r, g, b, a)
}

interface Shape {
    fun draw(g: Graphics2D)
    fun mutate()
}

data class Circle(var x: Int, var y: Int, var colour: Colour, private var radius: Int) : Shape {
    override fun draw(g: Graphics2D) {
        g.color = colour.getColor()
        g.fillOval(x - radius/2, y - radius/2, radius, radius)
    }

    override fun mutate() {
        x += Random().nextInt(10) - 5
        y += Random().nextInt(10) - 5
        radius += Random().nextInt(10) - 5
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

class Individual(
    var genome: List<Circle>,
    val masterImage: BufferedImage,
    val width: Int,
    val height: Int
) {
    var bufferedImage: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    var fitness = Integer.MAX_VALUE
    val masterPixels = grabPixels(masterImage)

    fun draw(g: Graphics2D) {
        for (shape in genome) {
            shape.draw(g)
        }
    }

    fun drawToBuffer() {
        val g = bufferedImage.createGraphics()
        g.setColor(Color.BLACK)
        g.fillRect(0, 0, width, height)

        draw(g)
    }

    fun drawAndCalculateFitness() {
        drawToBuffer()
        fitness = calculateFitness()
    }

    private fun calculateFitness(): Int {
        val pixels = grabPixels(bufferedImage)
        val pixelDistance = comparePixels(masterPixels, pixels)

        println(pixelDistance)

        return pixelDistance
    }

    private fun comparePixels(masterPixels: IntArray, pixels: IntArray): Int {
        if (masterPixels.size != pixels.size) {
            throw IllegalArgumentException("Oh my!")
        }

        var total: Int  = 0
        for ((i, pi) in pixels.withIndex()) {
            val mpi = masterPixels[i]

            val pr: Int  =  (pi shr 16) and 0xFF
            val pg: Int  =  (pi shr 8 ) and 0xFF
            val pb: Int  =  (pi       ) and 0xFF
            val mpr: Int = (mpi shr 16) and 0xFF
            val mpg: Int = (mpi shr 8 ) and 0xFF
            val mpb: Int = (mpi       ) and 0xFF

            val dr: Int = pr - mpr
            val dg: Int = pg - mpg
            val db: Int = pb - mpb

            total += ((dr*dr + dg*dg + db*db) shr 6)
        }

        return total

    }

    private fun grabPixels(image: BufferedImage): IntArray {
        val pixels = IntArray(width * height)
        val pixelGrabber = PixelGrabber(image, 0, 0, width, height, pixels, 0, width)
        pixelGrabber.grabPixels()
        return pixels
    }

    fun mutate() {
        genome.forEach { it.mutate() }
        drawAndCalculateFitness()
    }
}

private fun makeGUI(): GUI {
    var gui = GUI()

    return gui
}

fun main(args: Array<String>) {
    val gui = makeGUI()
    gui.isVisible = true
}

