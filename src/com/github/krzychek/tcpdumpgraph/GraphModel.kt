package com.github.krzychek.tcpdumpgraph

import com.mxgraph.model.mxCell
import com.mxgraph.view.mxGraph


class GraphModel() {
    val ipToVertexMap: Map<String, mxCell> = hashMapOf()
    val ipToIpEdgeMap: Map<String, mxCell> = hashMapOf()
}

class GraphNode(
        val id: NodeId,
        val stats: Stats,
        val children: MutableList<GraphNode> = mutableListOf()
)

class NodeId {
    constructor(ip: String) {
        name = ip
    }

    constructor(firstIp: String, between: Int, second: String)
            : this("$firstIp -> ${"* ".repeat(between)}-> $second")

    val name: String
}

class Stats(
        val rtt: Double
) {
    var totalSize: Int = 0
        private set
        get

    var hits: Int = 0
        private set
        get

    fun countHit() = hits++
}
