package com.github.krzychek.tcpdumpgraph.capture

import com.github.krzychek.tcpdumpgraph.capture.model.RouteCapture
import com.github.krzychek.tcpdumpgraph.capture.model.RouteNode
import com.github.krzychek.tcpdumpgraph.capture.model.TCPDumpCapture
import com.github.krzychek.tcpdumpgraph.killOnShutdown
import com.github.krzychek.tcpdumpgraph.model.Address
import com.github.krzychek.tcpdumpgraph.utils.kScheduleWithFixedDelay
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class RouteCreator
    : (TCPDumpCapture) -> CompletableFuture<RouteCapture> {

    private val routes: MutableMap<Address, List<RouteNode>> = ConcurrentHashMap()

    init {
        Executors.newSingleThreadScheduledExecutor().kScheduleWithFixedDelay(10, 10, TimeUnit.MINUTES) {
            routes.replaceAll { address, _ ->
                createRouteCapture(address)
            }
        }
    }

    override fun invoke(capture: TCPDumpCapture): CompletableFuture<RouteCapture> =
            routes[capture.address]?.let {
                completedFuture(RouteCapture(
                        lenght = capture.lenght,
                        incomming = capture.incomming,
                        nodes = it
                ))
            } ?: computeRoute(capture)


    val executor = Executors.newSingleThreadExecutor()
    private fun computeRoute(capture: TCPDumpCapture): CompletableFuture<RouteCapture> =
            CompletableFuture.supplyAsync(Supplier {
                RouteCapture(
                        lenght = capture.lenght,
                        incomming = capture.incomming,
                        nodes = routes.computeIfAbsent(capture.address) {
                            createRouteCapture(capture.address)
                        }
                )
            }, executor)

    private val getIpAddressOfNode: (String) -> String? = {
        " *\\d+ +(\\S+) ?.*".toRegex()
                .matchEntire(it)
                ?.groupValues?.get(1)
    }

    private val ROUTE_PREFIX_NODES = listOf(RouteNode.KnownRouteNode("localhost"))

    fun createRouteCapture(address: Address) =
            ROUTE_PREFIX_NODES +
                    ProcessBuilder(listOf("traceroute", "-n", address.name, "-q", "1")).start()
                            .killOnShutdown()
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
                                if (it.lastOrNull()?.address != address.name)
                                    it + RouteNode.KnownRouteNode(address.name)
                                else it
                            }


}

