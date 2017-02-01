package com.github.krzychek.tcpdumpgraph.graph

import com.github.krzychek.tcpdumpgraph.graph.model.Edge
import com.github.krzychek.tcpdumpgraph.graph.model.GraphModel
import com.github.krzychek.tcpdumpgraph.graph.model.Node
import com.github.krzychek.tcpdumpgraph.graph.model.VisualEdge
import com.github.krzychek.tcpdumpgraph.utils.kScheduleWithFixedDelay
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import org.jgrapht.Graph
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VisualGraphToModelBinder(kodein: Kodein) {

    private val graphModel: GraphModel = kodein.instance()
    private val graph: Graph<Node, VisualEdge> = kodein.instance()

    private val uiUpdaterService = Executors.newSingleThreadScheduledExecutor()

    fun start() {
        uiUpdaterService.kScheduleWithFixedDelay(0, 1, TimeUnit.SECONDS, {
            graphModel.edges.forEach { edge ->
                edge.from.updateOnGraph()
                edge.to.updateOnGraph()
                edge.updateOnGraph()
            }
            graphModel.nodes.forEach { it.updateOnGraph() }
        })
    }

    private fun Node.updateOnGraph() {
        graph.addVertex(this)
    }

    private fun Edge.updateOnGraph() {
        try {
            graph.addEdge(this.from, this.to)
        } catch (e: Exception) {
            println(e)
        }
    }
}

