package com.github.krzychek.tcpdumpgraph

import com.github.krzychek.tcpdumpgraph.graph.model.Node
import java.util.concurrent.atomic.AtomicLong

// TODO global state object: bleee!
class GlobalStateHolder {
    val waitingDumpsForRoute = AtomicLong(0)
    val packetsCaptured = AtomicLong(0)
    var selectedNode: Node? = null
}