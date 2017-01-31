package com.github.krzychek.tcpdumpgraph

import com.github.krzychek.tcpdumpgraph.capture.RouteCreator
import com.github.krzychek.tcpdumpgraph.capture.TCPDumpReader
import com.github.krzychek.tcpdumpgraph.capture.model.RouteCapture
import com.github.krzychek.tcpdumpgraph.graph.GraphModelUpdater
import com.github.krzychek.tcpdumpgraph.graph.VisualGraphToModelBinder
import com.github.krzychek.tcpdumpgraph.graph.model.EdgeId
import com.github.krzychek.tcpdumpgraph.graph.model.GraphModel
import com.github.krzychek.tcpdumpgraph.graph.model.Node
import com.github.krzychek.tcpdumpgraph.graph.model.VisualEdge
import com.github.krzychek.tcpdumpgraph.graph.startUpdatingGraphStats
import com.github.krzychek.tcpdumpgraph.utils.scheduleWithFixedDelayX
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.jgraph.layout.JGraphFacade
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout
import org.jgraph.JGraph
import org.jgraph.graph.DefaultGraphCell
import org.jgrapht.EdgeFactory
import org.jgrapht.ext.JGraphModelAdapter
import org.jgrapht.graph.DefaultDirectedWeightedGraph
import org.jgrapht.graph.ListenableDirectedWeightedGraph
import java.awt.Font
import java.awt.Point
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.swing.*
import javax.swing.JFrame.EXIT_ON_CLOSE
import javax.swing.tree.DefaultMutableTreeNode
import kotlin.concurrent.thread


val processToKill = mutableListOf<Process>()
fun Process.killOnShutdown() = apply {
    processToKill += this
}

fun main(args: Array<String>) {
    Runtime.getRuntime().addShutdownHook(Thread {
        processToKill.forEach { it.destroyForcibly() }
    })

    val kodein = Kodein {
        bind<GraphModel>() with singleton { GraphModel() }
        bind<TCPDumpReader>() with singleton { TCPDumpReader() }
        bind<RouteCreator>() with singleton { RouteCreator() }
        bind<GraphModelUpdater>() with singleton { GraphModelUpdater(kodein) }
        bind<StateHolder>() with singleton { StateHolder() }
        bind<JGraph>() with singleton { createJGraph(kodein) }
        bind<ListenableDirectedWeightedGraph<Node, VisualEdge>>() with singleton { createListenableDirectedWeghtedGraph(kodein) }
    }

    startGraphUI(kodein)
    startTCPDump(kodein)
    startUpdatingGraphStats(kodein)
    startControllForm(kodein)
}

class StateHolder {
    val waitingDumpsForRoute = AtomicLong(0)
    val packetsCaptured = AtomicLong(0)
    var selectedNode: Node? = null
}

val tcpDumpActiveToggle = AtomicBoolean(true)
fun startTCPDump(kodein: Kodein) = thread {

    val graphModelUpdater: GraphModelUpdater = kodein.instance()
    val routeCreator: RouteCreator = kodein.instance()
    val tcpDumpReader: TCPDumpReader = kodein.instance()
    val stateHolder: StateHolder = kodein.instance()

    ProcessBuilder("gksudo", "tcpdump tcp -t -n").start()
            .killOnShutdown()
            .apply {
                inputStream.reader().useLines {
                    tcpDumpReader.readFrom(it)
                            .filter { tcpDumpActiveToggle.get() }
                            .map(routeCreator)
                            .forEach { future: CompletableFuture<RouteCapture> ->
                                stateHolder.waitingDumpsForRoute.incrementAndGet()
                                future.thenAccept {
                                    stateHolder.packetsCaptured.incrementAndGet()
                                    stateHolder.waitingDumpsForRoute.decrementAndGet()
                                    graphModelUpdater.processRouteCapture(it)
                                }
                            }
                }
                waitFor()
                Runtime.getRuntime().exit(0)
            }
}


fun startControllForm(kodein: Kodein) {
    val jGraph: JGraph = kodein.instance()
    val stateHolder: StateHolder = kodein.instance()
    val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()


    fun Double.format(format: String = "%.2f") = String.format(format, this)
    fun Long.format(format: String = "%d") = String.format(format, this)

    JFrame("graph controll").apply {
        font = Font.getFont(Font.MONOSPACED)
        contentPane = JPanel().apply {

            add(JToggleButton("tcp dump active", tcpDumpActiveToggle.get()).apply {
                addActionListener {
                    tcpDumpActiveToggle.set((it.source as JToggleButton).model.isSelected)
                }
            })

            add(JButton("relayout").apply {
                val layout = JGraphHierarchicalLayout()
                val facade = JGraphFacade(jGraph).apply {
                    roots = getUnconnectedVertices(false).filter {
                        if (it is DefaultGraphCell) {
                            val userObject = it.userObject
                            if (userObject is Node && userObject.id.uniqueId == "localhost")
                                return@filter true
                        }
                        return@filter false
                    }

                }
                addActionListener {
                    layout.run(facade)
                    facade.createNestedMap(true, true).let { map ->
                        jGraph.graphLayoutCache.edit(map)
                    }
                }
            })

            add(JButton("zoom in").apply {
                addActionListener { jGraph.scale += 0.1 }
            })

            add(JButton("zomm out").apply {
                addActionListener { jGraph.scale -= 0.1 }
            })


            add(JTextArea().apply {
                executor.scheduleWithFixedDelayX(0, 200, TimeUnit.MILLISECONDS, {
                    this.text = """
                                    | === === PACKETS === ===
                                    |   in queue : ${stateHolder.waitingDumpsForRoute.get()}
                                    |   captured : ${stateHolder.packetsCaptured.get()}
                                    |""".trimMargin()
                })
            })


            add(JTextArea().apply {
                executor.scheduleWithFixedDelayX(0, 200, TimeUnit.MILLISECONDS, {
                    stateHolder.selectedNode?.let {
                        this.text = """
                                    | === === Node === ===
                                    |     name  : ${it.id.display}
                                    |       in  : ${it.inPacketCounter.totalDataSize} bytes / ${it.inPacketCounter.packetCount} packets
                                    |      out  : ${it.outPacketCounter.totalDataSize} bytes / ${it.outPacketCounter.packetCount} packets
                                    |""".trimMargin() +
                                if (it.rtt.count > 0) """
                                    |
                                    | === === RTT === ===
                                    |      avg : ${it.rtt.average.format()}
                                    |      max : ${it.rtt.max.format()}
                                    |      min : ${it.rtt.min.format()}
                                    |    count : ${it.rtt.count.format()}
                                    |""".trimMargin()
                                else ""
                    }
                })
            })
        }

        setSize(250, 400)
        isResizable = false
        isVisible = true
    }
}


fun startGraphUI(kodein: Kodein) = thread {
    val graphModel: GraphModel = kodein.instance()
    val listenableDirectedWeightedGraph: ListenableDirectedWeightedGraph<Node, VisualEdge> = kodein.instance()
    val jGraph: JGraph = kodein.instance()
    JFrame("Graph").apply {
        contentPane = JScrollPane(jGraph)
        setSize(500, 500)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
    }

    VisualGraphToModelBinder(graphModel = graphModel, graph = listenableDirectedWeightedGraph)
            .start()

}

private fun createJGraph(kodein: Kodein): JGraph {
    val listenableDirectedWeightedGraph: ListenableDirectedWeightedGraph<Node, VisualEdge> = kodein.instance()
    val stateHolder: StateHolder = kodein.instance()
    return JGraph(JGraphModelAdapter(listenableDirectedWeightedGraph)).apply {
        selectionModel.addGraphSelectionListener { event ->
            val cell = (event.cells.last() as? DefaultMutableTreeNode)
            stateHolder.selectedNode = (cell?.userObject as? Node)
        }
    }
}

private fun createListenableDirectedWeghtedGraph(kodein: Kodein): ListenableDirectedWeightedGraph<Node, VisualEdge> {
    val graphModel: GraphModel = kodein.instance()
    return ListenableDirectedWeightedGraph(
            DefaultDirectedWeightedGraph(EdgeFactory { (fromId), (toId) ->
                VisualEdge(graphModel.edgeMap[EdgeId(fromId, toId)]!!)
            })
    )
}


operator fun Point.component1() = x
operator fun Point.component2() = y
