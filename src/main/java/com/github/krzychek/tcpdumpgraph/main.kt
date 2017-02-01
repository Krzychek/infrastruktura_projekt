package com.github.krzychek.tcpdumpgraph

import com.github.krzychek.tcpdumpgraph.capture.RouteCreator
import com.github.krzychek.tcpdumpgraph.capture.TCPDumpReader
import com.github.krzychek.tcpdumpgraph.capture.TCPDumpReadingThread
import com.github.krzychek.tcpdumpgraph.graph.GraphModelUpdater
import com.github.krzychek.tcpdumpgraph.graph.NodeStatsReadingThread
import com.github.krzychek.tcpdumpgraph.graph.VisualGraphToModelBinder
import com.github.krzychek.tcpdumpgraph.graph.model.GraphModel
import com.github.krzychek.tcpdumpgraph.graph.model.Node
import com.github.krzychek.tcpdumpgraph.graph.model.VisualEdge
import com.github.salomonbrys.kodein.*
import org.jgraph.JGraph
import org.jgrapht.EdgeFactory
import org.jgrapht.Graph
import org.jgrapht.ext.JGraphModelAdapter
import org.jgrapht.graph.DefaultDirectedWeightedGraph
import org.jgrapht.graph.ListenableDirectedWeightedGraph
import java.awt.Point
import javax.swing.tree.DefaultMutableTreeNode


val processToKill = mutableListOf<Process>()
fun Process.killOnShutdown() = apply {
    processToKill += this
}

fun main(args: Array<String>) {
    Runtime.getRuntime().addShutdownHook(Thread {
        processToKill.forEach { it.destroyForcibly() }
    })

    Kodein {
        bind<TCPDumpReader>() with singleton { TCPDumpReader() }
        bind<RouteCreator>() with singleton { RouteCreator() }
        bind<GraphModel>() with singleton { GraphModel() }
        bind<GraphModelUpdater>() with singleton { GraphModelUpdater(kodein) }
        bind<GlobalStateHolder>() with singleton { GlobalStateHolder() }
        bind<JGraph>() with singleton { createJGraph(kodein) }
        bind<Graph<Node, VisualEdge>>() with singleton { createGraph(kodein) }

        bind<TCPDumpReadingThread>() with eagerSingleton { TCPDumpReadingThread(kodein).apply { start() } }
        bind<NodeStatsReadingThread>() with eagerSingleton { NodeStatsReadingThread(kodein).apply { start() } }
        bind<VisualGraphToModelBinder>() with eagerSingleton { VisualGraphToModelBinder(kodein).apply { start() } }
        bind<GraphUI>() with eagerSingleton { GraphUI(kodein).apply { start() } }
        bind<ControllForm>() with eagerSingleton { ControllForm(kodein) }
    }
}


private fun createJGraph(kodein: Kodein): JGraph {
    val graph: Graph<Node, VisualEdge> = kodein.instance()
    val globalStateHolder: GlobalStateHolder = kodein.instance()
    return JGraph(JGraphModelAdapter(graph)).apply {
        selectionModel.addGraphSelectionListener { event ->
            val cell = (event.cells.last() as? DefaultMutableTreeNode)
            globalStateHolder.selectedNode = (cell?.userObject as? Node)
        }
    }
}

private fun createGraph(kodein: Kodein): Graph<Node, VisualEdge> {
    val graphModel: GraphModel = kodein.instance()
    return ListenableDirectedWeightedGraph(
            DefaultDirectedWeightedGraph(EdgeFactory { (fromId), (toId) ->
                VisualEdge(graphModel.getEdge(fromId, toId)!!)
            })
    )
}


