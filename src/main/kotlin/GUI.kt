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
import javax.swing.Timer
import kotlin.system.measureTimeMillis

private val GENOME_SIZE = 1000

private val random = Random()
val minAlpha = 32
val maxAlpha = 64
val colourMutateAmount = 20

class GUI(title: String? = "Genetic!") : JFrame(title) {
    constructor(): this("Genetic!") {
        contentPane.add(UIPanel())
        pack()
    }

    class UIPanel(layout: LayoutManager): JPanel(layout) {
        val masterImage: BufferedImage = ImageIO.read(File("cow.jpg"))
        val masterIcon: ImageIcon  = ImageIcon(masterImage)

        constructor(): this(BorderLayout()) {
            val width = masterImage.width
            val height = masterImage.height

            val shapes = (1..GENOME_SIZE).map { spawnRandomShape(width, height) }

            val individual = Individual(shapes, masterImage, width, height)

            add(JLabel(masterIcon), BorderLayout.WEST)
            add(JLabel(ImageIcon(individual.bufferedImage)), BorderLayout.EAST)

            val mutateButton = JButton("Mutate!")
            add(mutateButton, BorderLayout.SOUTH)
            mutateButton.addActionListener { e ->
                individual.mutate()
                repaint()
            }

            startTimer(individual)
        }

        private fun startTimer(individual: Individual) {
            val timer = Timer(20) { e ->
                val timeInMillis = measureTimeMillis {
                    individual.mutate()
                    repaint()
                }
            }
            timer.setInitialDelay(1000)
            timer.start()
        }
    }
}

data class Colour(val r: Int, val g:Int, val b: Int, val a:Int) {
    fun getColor(): Color {
        try {
            return Color(r, g, b, a)
        } catch (e: Exception) {
            println("$this")
            throw e
        }
    }
    fun mutate(): Colour {
        val red = mutateValueLinear(r, colourMutateAmount, 0, 255)
        val green = mutateValueLinear(g, colourMutateAmount, 0, 255)
        val blue = mutateValueLinear(b, colourMutateAmount, 0, 255)
        val alpha = mutateValueLinear(a, colourMutateAmount, minAlpha, maxAlpha)

        val newColour = Colour(red, green, blue, alpha)
        return newColour
    }
}

interface Shape {
    fun draw(g: Graphics2D)
    fun mutate(): Circle
}


data class Circle(
    val x: Int,
    val y: Int,
    private val radius: Int,
    val colour: Colour,
    val width: Int,
    val height: Int
) : Shape {

    override fun draw(g: Graphics2D) {
        g.color = colour.getColor()
        g.fillOval(x - radius/2, y - radius/2, radius, radius)
    }

    override fun mutate(): Circle {
        val newX = mutateValueLinear(x, 5, 0, width)
        val newY = mutateValueLinear(y, 5, 0, height)
        val newRadius = mutateValueLinear(radius, 5, 1, 20)

        return Circle(newX, newY, newRadius, colour.mutate(), width, height)
    }
}

fun spawnRandomShape(width: Int, height: Int): Circle {
    val random = random
    return Circle(
        random.nextInt(width),
        random.nextInt(height),
        random.nextInt(50),
        Colour(random.nextInt(255), random.nextInt(255), random.nextInt(255), randint(minAlpha, maxAlpha)),
        width,
        height
    )
}

class Individual(
    var genome: List<Circle>,
    val masterImage: BufferedImage,
    val width: Int,
    val height: Int,
    var generation: Int = 1
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

        return pixelDistance
    }

    private fun comparePixels(masterPixels: IntArray, pixels: IntArray): Int {
        if (masterPixels.size != pixels.size) {
            throw IllegalArgumentException("Oh my!")
        }

        var total = 0
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
        val newGenome = genome.map { it.mutate() }
        val newIndividual = Individual(newGenome, masterImage, width, height, generation + 1)
        newIndividual.drawAndCalculateFitness()

        this.generation++

        if (newIndividual.fitness < this.fitness) {
            this.genome = newIndividual.genome
            this.drawAndCalculateFitness()
            println("new fitness: ${this.fitness}")

            val outputFile = File(String.format("output/cow_%010d.png", newIndividual.generation))
            ImageIO.write(newIndividual.bufferedImage, "png", outputFile)
        }
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

private fun randint(min: Int, max: Int): Int = random.nextInt(max - min) + min

private fun mutateValueLinear(oldValue: Int, range: Int, min: Int, max: Int): Int {
    if (randint(0, 10000) < 5) {
        var newValue = oldValue + random.nextInt(range * 2) - range
        if (newValue < min) newValue = min
        if (newValue > max) newValue = max
        return newValue
    } else {
        return oldValue
    }
}
