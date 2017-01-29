package com.github.krzychek.tcpdumpgraph.graph.model

import java.util.concurrent.ConcurrentHashMap


class Graph {

    val nodeMap: MutableMap<NodeId, Node> = ConcurrentHashMap(hashMapOf(NodeId("localhost") to Node(NodeId("localhost"))))
    val edgeMap: MutableMap<EdgeId, Edge> = ConcurrentHashMap()
    val edges: Collection<Edge>
        get() = edgeMap.values

    fun createNode(nodeId: NodeId) =
            nodeMap.computeIfAbsent(nodeId) { Node(nodeId) }

    fun createEdge(fromNode: Node, toNode: Node) =
            edgeMap.computeIfAbsent(EdgeId(fromNode.id, toNode.id)) {
                Edge(fromNode, toNode)
            }

}

