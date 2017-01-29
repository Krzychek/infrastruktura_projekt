package com.github.krzychek.tcpdumpgraph.graph

import com.github.krzychek.tcpdumpgraph.graph.model.Edge
import com.github.krzychek.tcpdumpgraph.graph.model.Graph
import com.github.krzychek.tcpdumpgraph.graph.model.Node
import com.github.krzychek.tcpdumpgraph.graph.model.VisualEdge
import com.github.krzychek.tcpdumpgraph.utils.scheduleWithFixedDelayNoDalay
import org.jgrapht.graph.ListenableDirectedWeightedGraph
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VisualGraphUpdater(private val graphModel: Graph,
                         private val graph: ListenableDirectedWeightedGraph<Node, VisualEdge>) {

    private val uiUpdaterService = Executors.newSingleThreadScheduledExecutor()

    fun startWithUpdateCallback(afterUpdate: () -> Unit) {
        uiUpdaterService.scheduleWithFixedDelayNoDalay(1, TimeUnit.SECONDS) {
            graphModel.edges.forEach { edge ->
                edge.from.updateOnGraph()
                edge.to.updateOnGraph()
                edge.updateOnGraph()
            }
            graphModel.nodeMap.values.forEach { it.updateOnGraph() }
            afterUpdate()
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

