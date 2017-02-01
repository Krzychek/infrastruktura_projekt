package com.github.krzychek.tcpdumpgraph

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import org.jgraph.JGraph
import javax.swing.JFrame
import javax.swing.JFrame.EXIT_ON_CLOSE
import javax.swing.JScrollPane

class GraphUI(kodein: Kodein) : Thread("GraphUI") {

    private val jGraph: JGraph = kodein.instance()

    override fun run() {
        JFrame("Graph").apply {
            contentPane = JScrollPane(jGraph)
            setSize(500, 500)
            defaultCloseOperation = EXIT_ON_CLOSE
            isVisible = true
        }
    }

}