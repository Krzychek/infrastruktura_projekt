package com.github.krzychek.tcpdumpgraph.capture.model

data class RouteCapture(
        val lenght: Int,
        val incomming: Boolean,
        val nodes: List<RouteNode>
)

