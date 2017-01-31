package com.github.krzychek.tcpdumpgraph.capture

import com.github.krzychek.tcpdumpgraph.capture.model.RouteCapture
import com.github.krzychek.tcpdumpgraph.capture.model.RouteNode
import com.github.krzychek.tcpdumpgraph.capture.model.TCPDumpCapture
import com.github.krzychek.tcpdumpgraph.killOnShutdown
import com.github.krzychek.tcpdumpgraph.model.Address
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.ConcurrentHashMap

class RouteCreator
    : (TCPDumpCapture) -> CompletableFuture<RouteCapture> {

    private val routes: MutableMap<Address, RouteCapture> = ConcurrentHashMap()

    override fun invoke(capture: TCPDumpCapture): CompletableFuture<RouteCapture> =
            routes[capture.address]?.let { completedFuture(it) } ?: computeRoute(capture)


    private fun computeRoute(capture: TCPDumpCapture): CompletableFuture<RouteCapture> =
            CompletableFuture.supplyAsync {
                routes.computeIfAbsent(capture.address) { createRouteCapture(capture) }
            }

    private val getIpAddressOfNode: (String) -> String? = {
        " *\\d+ +(\\S+) ?.*".toRegex()
                .matchEntire(it)
                ?.groupValues?.get(1)
    }

    private val ROUTE_PREFIX_NODES = listOf(RouteNode.KnownRouteNode("localhost"))

    fun createRouteCapture(capture: TCPDumpCapture) = RouteCapture(
            lenght = capture.lenght,
            incomming = capture.incomming,
            nodes = ROUTE_PREFIX_NODES +
                    ProcessBuilder(listOf("traceroute", "-n", capture.address.name, "-q", "1")).start()
                            .killOnShutdown()
                            .apply { waitFor() }
                            .inputStream.bufferedReader().readLines()
                            .map(getIpAddressOfNode)
                            .filterNotNull()
                            .map {
                                if ('*' in it) RouteNode.UnknownRouteNode()
                                else RouteNode.KnownRouteNode(it)
                            }
                            .fold(emptyList<RouteNode>()) { list, routeNode ->
                                val last = list.lastOrNull()
                                if (routeNode is RouteNode.UnknownRouteNode && last is RouteNode.UnknownRouteNode)
                                    list.apply { last.count++ }
                                else list + routeNode
                            }
                            .let {
                                if (it.lastOrNull()?.address != capture.address.name)
                                    it + RouteNode.KnownRouteNode(capture.address.name)
                                else it
                            }
                            .let {
                                if (capture.incomming) it.reversed() else it
                            }
    )


}

