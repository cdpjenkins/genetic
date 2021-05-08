package com.cdpjenkins.genetic.svg

import com.cdpjenkins.genetic.model.Individual
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import java.awt.Dimension
import java.io.*

class SvgRenderer {
    fun renderToFile(file: File, individual: Individual) {
        val svgGenerator = draw(individual)

        OutputStreamWriter(FileOutputStream(file), "UTF-8").use {
            svgGenerator.stream(it, true)
        }
    }

    fun renderToString(individual: Individual?): String {
        if (individual == null)  {
            return ""
        } else {
            val svgGenerator = draw(individual)

            val bytes = ByteArrayOutputStream()
            svgGenerator.stream(OutputStreamWriter(bytes))
            return String(bytes.toByteArray())
        }
    }

    private fun draw(individual: Individual): SVGGraphics2D {
        val domImpl = GenericDOMImplementation.getDOMImplementation()
        val svgNS = "http://www.w3.org/2000/svg"
        val document = domImpl.createDocument(svgNS, "svg", null)
        val svgGenerator = SVGGraphics2D(document)
        svgGenerator.svgCanvasSize = Dimension(individual.bounds.width, individual.bounds.height)
        individual.draw(svgGenerator)
        return svgGenerator
    }
}
