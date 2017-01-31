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
import com.jgraph.layout.JGraphFacade
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout
import org.jgraph.JGraph
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


val localHostIPs by lazy {
    ProcessBuilder("ip", "add", "show").start().killOnShutdown()
            .inputStream.bufferedReader().readLines()
            .map { ".*inet (.+)/.*".toRegex().matchEntire(it)?.groupValues?.get(1) }
            .filterNotNull()
}

val processToKill = mutableListOf<Process>()
fun Process.killOnShutdown() = apply {
    processToKill += this
}

fun main(args: Array<String>) {
    Runtime.getRuntime().addShutdownHook(Thread {
        processToKill.forEach { it.destroyForcibly() }
    })

    val graphModel = GraphModel()
    val tcpDumpReader = TCPDumpReader(isIncomming = { it in localHostIPs }) // TODO replace
    val routeCreator = RouteCreator()
    val graphModelUpdater = GraphModelUpdater(graphModel = graphModel)

    startTCPDump(graphModelUpdater, routeCreator, tcpDumpReader)
    startGraphUI(graphModel)
    startUpdatingGraphStats(graphModel)
}

val waitingDumpsForRoute = AtomicLong(0)
val packetsCaptured = AtomicLong(0)
var selectedNode: Node? = null

val tcpDumpActiveToggle = AtomicBoolean(true)
fun startTCPDump(graphModelUpdater: GraphModelUpdater, routeCreator: RouteCreator, tcpDumpReader: TCPDumpReader) = thread {
    ProcessBuilder("gksudo", "tcpdump tcp -t -n").start()
            .killOnShutdown()
            .apply {
                inputStream.bufferedReader().useLines {
                    tcpDumpReader.readFrom(it)
                            .filter { tcpDumpActiveToggle.get() }
                            .map(routeCreator)
                            .forEach { future: CompletableFuture<RouteCapture> ->
                                waitingDumpsForRoute.incrementAndGet()
                                future.thenAccept {
                                    packetsCaptured.incrementAndGet()
                                    waitingDumpsForRoute.decrementAndGet()
                                    graphModelUpdater.processRouteCapture(it)
                                }
                            }
                }
                waitFor()
                Runtime.getRuntime().exit(0)
            }
}


fun startControllForm(jGraph: JGraph) {
    val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

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
                val facade = JGraphFacade(jGraph)
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
                executor.scheduleWithFixedDelayX(200, TimeUnit.MILLISECONDS) {
                    this.text = """
                                | === === PACKETS === ===
                                |   in queue : ${waitingDumpsForRoute.get()}
                                |   captured : ${packetsCaptured.get()}
                                |""".trimMargin()
                }
            })


            add(JTextArea().apply {
                executor.scheduleWithFixedDelayX(200, TimeUnit.MILLISECONDS) {
                    selectedNode?.let {
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
                }
            })
        }

        setSize(200, 400)
        isResizable = false
        isVisible = true
    }
}

fun Double.format(format: String = "%.2f") {
    String.format(format, this)
}

fun Long.format(format: String = "%d") {
    String.format(format, this)
}

fun startGraphUI(graphModel: GraphModel) = thread {
    val listenableDirectedWeightedGraph = ListenableDirectedWeightedGraph<Node, VisualEdge>(
            DefaultDirectedWeightedGraph(EdgeFactory { (fromId), (toId) ->
                VisualEdge(graphModel.edgeMap[EdgeId(fromId, toId)]!!)
            }))
    val jGraph = JGraph(JGraphModelAdapter(listenableDirectedWeightedGraph)).apply {
        selectionModel.addGraphSelectionListener { event ->
            val cell = (event.cells.last() as? DefaultMutableTreeNode)
            selectedNode = (cell?.userObject as? Node)
        }
    }
    JFrame("Graph").apply {
        contentPane = JScrollPane(jGraph)
        setSize(500, 500)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
    }


    VisualGraphToModelBinder(graphModel = graphModel, graph = listenableDirectedWeightedGraph)
            .start()

    startControllForm(jGraph)
}


operator fun Point.component1() = x
operator fun Point.component2() = y
