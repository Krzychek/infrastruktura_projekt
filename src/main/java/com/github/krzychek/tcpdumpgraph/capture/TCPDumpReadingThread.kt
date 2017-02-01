package com.github.krzychek.tcpdumpgraph.capture

import com.github.krzychek.tcpdumpgraph.GlobalStateHolder
import com.github.krzychek.tcpdumpgraph.capture.RouteCreator
import com.github.krzychek.tcpdumpgraph.capture.TCPDumpReader
import com.github.krzychek.tcpdumpgraph.capture.model.RouteCapture
import com.github.krzychek.tcpdumpgraph.graph.GraphModelUpdater
import com.github.krzychek.tcpdumpgraph.killOnShutdown
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class TCPDumpReadingThread(kodein: Kodein) : Thread("TCPDumpReadingThread") {

    private val graphModelUpdater: GraphModelUpdater = kodein.instance()
    private val routeCreator: RouteCreator = kodein.instance()
    private val tcpDumpReader: TCPDumpReader = kodein.instance()
    private val globalStateHolder: GlobalStateHolder = kodein.instance()

    val tcpDumpReadingActive = AtomicBoolean(true)

    override fun run() {
        ProcessBuilder("gksudo", "tcpdump tcp -t -n").start()
                .killOnShutdown()
                .inputStream.reader()
                .useLines {
                    tcpDumpReader.readFrom(it)
                            .filter { tcpDumpReadingActive.get() }
                            .map(routeCreator)
                            .forEach { future: CompletableFuture<RouteCapture> ->
                                globalStateHolder.waitingDumpsForRoute.incrementAndGet()
                                future.thenAccept {
                                    globalStateHolder.packetsCaptured.incrementAndGet()
                                    globalStateHolder.waitingDumpsForRoute.decrementAndGet()
                                    graphModelUpdater.processRouteCapture(it)
                                }
                            }
                }
        // tcp dump killed
        Runtime.getRuntime().exit(0)
    }
}