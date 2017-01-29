package com.github.krzychek.tcpdumpgraph.graph.model

class Edge(
        val from: Node, val to: Node
) {

    val id: EdgeId = EdgeId(from.id, to.id)

    var totalDataSize: Int = 0
        get private set
    var packetCount: Int = 0
        get private set

    fun countPacket(size: Int) {
        totalDataSize += size
        packetCount++
    }

}