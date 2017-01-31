package com.github.krzychek.tcpdumpgraph.graph

import com.github.krzychek.tcpdumpgraph.capture.model.RouteCapture
import com.github.krzychek.tcpdumpgraph.capture.model.RouteNode
import com.github.krzychek.tcpdumpgraph.capture.model.RouteNode.KnownRouteNode
import com.github.krzychek.tcpdumpgraph.capture.model.RouteNode.UnknownRouteNode
import com.github.krzychek.tcpdumpgraph.graph.model.GraphModel
import com.github.krzychek.tcpdumpgraph.graph.model.Node
import com.github.krzychek.tcpdumpgraph.graph.model.NodeId
import com.github.krzychek.tcpdumpgraph.utils.NOT_IMPLEMENTED
import com.github.krzychek.tcpdumpgraph.utils.iterateInPairs
import com.github.krzychek.tcpdumpgraph.utils.tripleMap
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance

class GraphModelUpdater(kodein: Kodein) {

    private val graphModel: GraphModel = kodein.instance()

    fun processRouteCapture(routeCapture: RouteCapture) {

        val nodes = if (routeCapture.incomming) routeCapture.nodes else routeCapture.nodes.reversed()
        mapToSequenceOfGraphNodes(nodes).toList().apply {
            if (routeCapture.incomming) {
                forEach { it.inPacketCounter.countPacket(routeCapture.lenght) }
            } else {
                forEach { it.outPacketCounter.countPacket(routeCapture.lenght) }
            }

            iterateInPairs { prev, current ->
                graphModel.createEdge(prev, current)
                        .packetCounter
                        .countPacket(routeCapture.lenght)
            }
        }

    }

    private fun mapToSequenceOfGraphNodes(nodes: List<RouteNode>): Sequence<Node> =
            nodes.tripleMap { previous, current, next ->
                val nodeId: NodeId = current.createNodeId(previous, next)
                graphModel.createNode(nodeId)
            }

    private fun RouteNode.createNodeId(previous: RouteNode? = null, next: RouteNode? = null): NodeId {
        return when (this) {
            is UnknownRouteNode -> NodeId(previous!!.createNodeId(), this.count, next!!.createNodeId())
            is KnownRouteNode -> NodeId(this.address)
            else -> throw NOT_IMPLEMENTED
        }
    }
}
