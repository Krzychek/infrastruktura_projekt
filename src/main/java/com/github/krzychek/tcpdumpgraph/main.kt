package com.github.krzychek.tcpdumpgraph

import com.github.krzychek.tcpdumpgraph.capture.RouteCreator
import com.github.krzychek.tcpdumpgraph.capture.TCPDumpReader


fun main(args: Array<String>) {
    val tcpDumpReader = TCPDumpReader(isIncomming = { it == "192.168.0.46" })

    val routeCreator = RouteCreator()


    ProcessBuilder("gksudo", "tcpdump tcp -t -n").start().apply {
        inputStream.bufferedReader().useLines {
            it.map(tcpDumpReader)
                    .filterNotNull()
                    .map(routeCreator)
                    .forEach {
                        it.thenApply(::println)
                    }
        }
    }
}