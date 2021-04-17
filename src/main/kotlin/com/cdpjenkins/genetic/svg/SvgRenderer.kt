package com.cdpjenkins.genetic.svg

import com.cdpjenkins.genetic.model.Individual
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import java.awt.Dimension
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class SvgRenderer {
    fun render(file: File, it: Individual) {
        val domImpl = GenericDOMImplementation.getDOMImplementation()
        val svgNS = "http://www.w3.org/2000/svg"
        val document = domImpl.createDocument(svgNS, "svg", null)
        val svgGenerator = SVGGraphics2D(document)
        svgGenerator.svgCanvasSize = Dimension(it.bounds.width, it.bounds.height)
        it.draw(svgGenerator)

        // TODO that thing that is a bit like try with resources
        val out = OutputStreamWriter(FileOutputStream(file), "UTF-8")
        svgGenerator.stream(out, true)
    }
}
