package com.github.krzychek.tcpdumpgraph.graph.model

class PacketCounter {

    var totalDataSize: Int = 0
        get private set
    var packetCount: Int = 0
        get private set

    fun countPacket(size: Int) {
        val TCP_HEADER_SIZE = 20
        totalDataSize += (size + TCP_HEADER_SIZE)
        packetCount++
    }
}