package com.github.krzychek.tcpdumpgraph

import com.github.krzychek.tcpdumpgraph.capture.TCPDumpReadingThread
import com.github.krzychek.tcpdumpgraph.graph.model.Node
import com.github.krzychek.tcpdumpgraph.utils.kScheduleWithFixedDelay
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.jgraph.layout.JGraphFacade
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout
import org.jgraph.JGraph
import org.jgraph.graph.DefaultGraphCell
import java.awt.Font
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.swing.*

class ControllForm(kodein: Kodein) : JFrame("graph controll") {

    private val jGraph: JGraph = kodein.instance()
    private val globalStateHolder: GlobalStateHolder = kodein.instance()
    private val tcpDumpReadingThread: TCPDumpReadingThread = kodein.instance()
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    init {
        font = Font.getFont(Font.MONOSPACED)
        contentPane = JPanel().apply {

            add(JToggleButton("tcp dump active", tcpDumpReadingThread.tcpDumpReadingActive.get()).apply {
                addActionListener {
                    tcpDumpReadingThread.tcpDumpReadingActive
                            .set((it.source as JToggleButton).model.isSelected)
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
                executor.kScheduleWithFixedDelay(0, 200, TimeUnit.MILLISECONDS, {
                    this.text = """
                                    | === === PACKETS === ===
                                    |   in queue : ${globalStateHolder.waitingDumpsForRoute.get()}
                                    |   captured : ${globalStateHolder.packetsCaptured.get()}
                                    |""".trimMargin()
                })
            })


            add(JTextArea().apply {
                executor.kScheduleWithFixedDelay(0, 200, TimeUnit.MILLISECONDS, {
                    globalStateHolder.selectedNode?.let {
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

    fun Double.format(format: String = "%.2f") = String.format(format, this)
    fun Long.format(format: String = "%d") = String.format(format, this)

}