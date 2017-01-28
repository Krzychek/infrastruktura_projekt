package com.github.krzychek.tcpdumpgraph.capture

import com.github.krzychek.tcpdumpgraph.capture.model.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Supplier

class RouteCreator
    : (TCPDumpCapture) -> CompletableFuture<RouteCapture> {

    private val routes: HashMap<Address, RouteCapture> = hashMapOf()

    override fun invoke(capture: TCPDumpCapture): CompletableFuture<RouteCapture> =
            routes[capture.address]?.let { completedFuture(it) } ?: computeRoute(capture)


    private val routeComputingExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private fun computeRoute(capture: TCPDumpCapture): CompletableFuture<RouteCapture> =
            supplyAsync(Supplier {
                routes.computeIfAbsent(capture.address) { createRouteCapture(capture) }
            }, routeComputingExecutor)

    private val getIpAddressOfNode: (String) -> String? = {
        " *\\d+ +(\\S+) ?.*".toRegex()
                .matchEntire(it)
                ?.groupValues?.get(1)
    }

    fun createRouteCapture(capture: TCPDumpCapture) = RouteCapture(
            lenght = capture.lenght,
            incomming = capture.incomming,
            nodes = ProcessBuilder(listOf("traceroute", "-n", capture.address.name, "-q", "1")).start().apply { waitFor() }
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
    )


}

