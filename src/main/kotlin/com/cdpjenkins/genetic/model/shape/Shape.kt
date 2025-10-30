package com.cdpjenkins.genetic.model.shape

import com.cdpjenkins.genetic.evolver.EvolverSettings
import com.cdpjenkins.genetic.evolver.mutateValueGaussian
import com.cdpjenkins.genetic.evolver.withProbability
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.geom.CubicCurve2D
import java.awt.geom.GeneralPath

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = QuadCurveShape::class, name = "QuadCurveShape"),
    JsonSubTypes.Type(value = StrokedCubicCurveShape::class, name = "StrokedCubicCurveShape"),
    JsonSubTypes.Type(value = Circle::class, name = "Circle"),
    JsonSubTypes.Type(value = RectangleShape::class, name = "RectangleShape"),
    JsonSubTypes.Type(value = PolygonShape::class, name = "PolygonShape"),
)
sealed interface Shape {
    fun draw(g: Graphics2D)
    fun mutate(evolverSettings: EvolverSettings): Shape
    fun maybeMutate(mutateProbability: Double, evolverSettings: EvolverSettings): Shape =
        if (withProbability(mutateProbability)) mutate(evolverSettings) else this
}

abstract class GeneralPathShape(
    val path: List<Point>,
    val colour: Colour
) : Shape {
    protected val generalPath: GeneralPath by lazy(this::makeGeneralPath)

    abstract fun makeGeneralPath(): GeneralPath

    override fun draw(g: Graphics2D) {
        g.color = colour.toAwtColor()
        g.fill(generalPath)
    }
}

data class QuadCurveShape(
    val quadCurvePath: List<Point>,
    val quadCurveColour: Colour,
    val bounds: BoundsRectangle
): GeneralPathShape(
    quadCurvePath,
    quadCurveColour
) {
    override fun makeGeneralPath(): GeneralPath {
        val generalPath = GeneralPath(GeneralPath.WIND_EVEN_ODD, quadCurvePath.size)
        generalPath.moveTo(quadCurvePath[quadCurvePath.size - 1].x.toDouble(), quadCurvePath[quadCurvePath.size - 1].y.toDouble())

        for (chunk in quadCurvePath.chunked(2)) {
            val p1 = chunk[0]
            val p2 = chunk[1]
            generalPath.quadTo(p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble())
        }

        return generalPath
    }

    override fun mutate(evolverSettings: EvolverSettings): Shape {
        val newPath = quadCurvePath.map { it.mutate(bounds, evolverSettings) }
        val newColour = quadCurveColour.mutate(evolverSettings)

        return QuadCurveShape(newPath, newColour, bounds)
    }
}

@JvmInline
value class StrokeWidth(val value: Float) {
    fun mutate(evolverSettings: EvolverSettings) = StrokeWidth(
        mutateValueGaussian(
            this.value.toInt(),
            2,
            (evolverSettings.minStrokeWidth ?: StrokedCubicCurveShape.DEFAULT_MIN_STROKE_WIDTH).toInt(),
            (evolverSettings.maxStrokeWidth ?: StrokedCubicCurveShape.DEFAULT_MAX_STROKE_WIDTH).toInt()
        ).toFloat()
    )
}

data class StrokedCubicCurveShape(
    val path: List<Point>,
    val colour: Colour,
    val bounds: BoundsRectangle,
    val strokeWidth: StrokeWidth? = null
): Shape {
    override fun draw(g: Graphics2D) {
        val p1 = path[0]
        val p2 = path[1]
        val p3 = path[2]
        val p4 = path[3]

        val curve = CubicCurve2D.Float(
            p1.x.toFloat(), p1.y.toFloat(),
            p2.x.toFloat(), p2.y.toFloat(),
            p3.x.toFloat(), p3.y.toFloat(),
            p4.x.toFloat(), p4.y.toFloat())
        val actualStrokeWidth = strokeWidth?.value ?: DEFAULT_STROKE_WIDTH
        val basicStroke = BasicStroke(actualStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.stroke = basicStroke
        g.color = colour.toAwtColor()
        g.draw(curve)
    }

    override fun mutate(evolverSettings: EvolverSettings): Shape {
        val newPath = path.map { it.mutate(bounds, evolverSettings) }
        val newColour = colour.mutate(evolverSettings)
        val newStrokeWidth = (strokeWidth ?: StrokeWidth(DEFAULT_STROKE_WIDTH)).mutate(evolverSettings)

        return StrokedCubicCurveShape(newPath, newColour, bounds, newStrokeWidth)
    }

    companion object {
        const val DEFAULT_STROKE_WIDTH = 4f
        const val DEFAULT_MIN_STROKE_WIDTH = 4f
        const val DEFAULT_MAX_STROKE_WIDTH = 32f
    }
}

data class Circle(
    val centre: Point,
    val radius: Radius,
    val colour: Colour,
    val bounds: BoundsRectangle
) : Shape {
    override fun draw(g: Graphics2D) {
        g.color = colour.toAwtColor()
        g.fillOval(centre.x - radius, centre.y - radius, radius*2, radius*2)
    }

    override fun mutate(evolverSettings: EvolverSettings): Circle {
        return Circle(
            centre.mutate(bounds, evolverSettings),
            radius.mutate(),
            colour.mutate(evolverSettings),
            bounds
        )
    }
}

typealias Radius = Int
fun Radius.mutate() = mutateValueGaussian(this, 2, 1, 20)

data class RectangleShape(
    val topLeft: Point,
    val bottomRight: Point,
    val colour: Colour,
    val bounds: BoundsRectangle
): Shape {
    override fun draw(g: Graphics2D) {
        g.color = colour.toAwtColor()
        g.fillRect(topLeft.x, topLeft.y, bottomRight.x-topLeft.x, bottomRight.y-topLeft.y)
    }

    override fun mutate(evolverSettings: EvolverSettings): Shape {
        val newTopLeft = topLeft.mutate(bounds, evolverSettings)
        val newBottomRight = bottomRight.mutate(bounds, evolverSettings)
        val newColour = colour.mutate(evolverSettings)
        return RectangleShape(newTopLeft, newBottomRight, newColour, bounds)
    }
}

class PolygonShape(
    path: List<Point>,
    colour: Colour,
    val bounds: BoundsRectangle
) : GeneralPathShape(path, colour) {
    override fun makeGeneralPath(): GeneralPath {
        val generalPath = GeneralPath(GeneralPath.WIND_EVEN_ODD, path.size)
        generalPath.moveTo(path[path.size - 1].x.toDouble(), path[path.size - 1].y.toDouble())

        for (point in path) {
            generalPath.lineTo(point.x.toDouble(), point.y.toDouble())
        }

        return generalPath
    }

    override fun mutate(evolverSettings: EvolverSettings): Shape {
        val newPath = path.map { it.mutate(bounds, evolverSettings) }
        val newColour = colour.mutate(evolverSettings)

        return PolygonShape(newPath, newColour, bounds)
    }
}
