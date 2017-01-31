package com.github.krzychek.tcpdumpgraph.graph

import com.github.krzychek.tcpdumpgraph.graph.model.Edge
import com.github.krzychek.tcpdumpgraph.graph.model.GraphModel
import com.github.krzychek.tcpdumpgraph.graph.model.Node
import com.github.krzychek.tcpdumpgraph.graph.model.VisualEdge
import com.github.krzychek.tcpdumpgraph.utils.scheduleWithFixedDelayX
import org.jgrapht.graph.ListenableDirectedWeightedGraph
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VisualGraphToModelBinder(private val graphModel: GraphModel,
                               private val graph: ListenableDirectedWeightedGraph<Node, VisualEdge>) {

    private val uiUpdaterService = Executors.newSingleThreadScheduledExecutor()

    fun start() {
        uiUpdaterService.scheduleWithFixedDelayX(1, TimeUnit.SECONDS) {
            graphModel.edges.forEach { edge ->
                edge.from.updateOnGraph()
                edge.to.updateOnGraph()
                edge.updateOnGraph()
            }
            graphModel.nodes.forEach { it.updateOnGraph() }
        }
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

