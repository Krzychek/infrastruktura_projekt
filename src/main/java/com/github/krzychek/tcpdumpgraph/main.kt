package com.github.krzychek.tcpdumpgraph

import com.github.krzychek.tcpdumpgraph.capture.RouteCreator
import com.github.krzychek.tcpdumpgraph.capture.TCPDumpReader
import com.github.krzychek.tcpdumpgraph.capture.model.RouteCapture
import com.github.krzychek.tcpdumpgraph.graph.GraphModelUpdater
import com.github.krzychek.tcpdumpgraph.graph.VisualGraphUpdater
import com.github.krzychek.tcpdumpgraph.graph.model.EdgeId
import com.github.krzychek.tcpdumpgraph.graph.model.Graph
import com.github.krzychek.tcpdumpgraph.graph.model.Node
import com.github.krzychek.tcpdumpgraph.graph.model.VisualEdge
import com.jgraph.layout.JGraphFacade
import com.jgraph.layout.JGraphLayout
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout
import com.jgraph.layout.organic.JGraphFastOrganicLayout
import com.jgraph.layout.organic.JGraphSelfOrganizingOrganicLayout
import com.jgraph.layout.tree.JGraphCompactTreeLayout
import org.jgraph.JGraph
import org.jgrapht.EdgeFactory
import org.jgrapht.WeightedGraph
import org.jgrapht.ext.JGraphModelAdapter
import org.jgrapht.graph.DefaultDirectedWeightedGraph
import org.jgrapht.graph.ListenableDirectedWeightedGraph
import java.util.concurrent.CompletableFuture
import javax.swing.JFrame
import javax.swing.JFrame.EXIT_ON_CLOSE
import javax.swing.JScrollPane
import kotlin.concurrent.thread


private val localhost = "192.168.0.46"


private val processToKill = mutableListOf<Process>()
fun Process.killOnShutdown() = apply {
    processToKill += this
}

fun main(args: Array<String>) {
    Runtime.getRuntime().addShutdownHook(Thread {
        processToKill.forEach { it.destroyForcibly() }
    })

    val graphModel = Graph()
    val tcpDumpReader = TCPDumpReader(isIncomming = { it == localhost }) // TODO replace
    val routeCreator = RouteCreator()
    val graphModelUpdater = GraphModelUpdater(graph = graphModel)

    startTCPDump(graphModelUpdater, routeCreator, tcpDumpReader)
    startUI(graphModel)
}


private fun startTCPDump(graphModelUpdater: GraphModelUpdater, routeCreator: RouteCreator, tcpDumpReader: TCPDumpReader) = thread {
    ProcessBuilder("gksudo", "tcpdump tcp -t -n").start()
            .killOnShutdown()
            .apply {
                inputStream.bufferedReader().useLines {
                    tcpDumpReader.readFrom(it)
                            .map(routeCreator)
                            .forEach { future: CompletableFuture<RouteCapture> ->
                                future.thenAccept { graphModelUpdater.processRouteCapture(it) }
                            }
                }
            }
            .apply {
                waitFor()
                Runtime.getRuntime().exit(0)
            }
}


private fun startUI(graphModel: Graph) = thread {
    val weightedGrapg: WeightedGraph<Node, VisualEdge> = DefaultDirectedWeightedGraph(EdgeFactory { (fromId), (toId) ->
        VisualEdge(graphModel.edgeMap[EdgeId(fromId, toId)]!!)
    })
    val listenableDirectedWeightedGraph = ListenableDirectedWeightedGraph<Node, VisualEdge>(weightedGrapg)
    val jGraphModelAdapter = JGraphModelAdapter(listenableDirectedWeightedGraph)
    val jGraph = JGraph(jGraphModelAdapter).apply {
        autoscrolls = true
    }
    JFrame("Graph").apply {
        contentPane.add(JScrollPane(jGraph))
        setSize(200, 200)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
    }
    val layout: JGraphLayout = JGraphHierarchicalLayout()
    val facade = JGraphFacade(jGraph)

    VisualGraphUpdater(graphModel = graphModel, graph = listenableDirectedWeightedGraph)
            .startWithUpdateCallback {
                layout.run(facade)
                facade.createNestedMap(true, true).let { map ->
                    jGraph.graphLayoutCache.edit(map)
                }
            }
}
