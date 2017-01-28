package com.github.krzychek.tcpdumpgraph.capture.model

interface RouteNode {
    val address: String?

    data class KnownRouteNode(override val address: String)
        : RouteNode

    data class UnknownRouteNode(var count: Int = 1)
        : RouteNode {
        override val address: String?
            get() = null
    }

}