package com.github.krzychek.tcpdumpgraph.graph.model

import com.github.krzychek.tcpdumpgraph.graph.model.Edge

class VisualEdge(private val edge: Edge) {
    override fun toString(): String {
        return "${edge.totalDataSize}B/${edge.packetCount}"
    }
}