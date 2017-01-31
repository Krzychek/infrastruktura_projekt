package com.github.krzychek.tcpdumpgraph.graph.model

class Edge(
        val from: Node, val to: Node
) {
    val packetCounter = PacketCounter()
}