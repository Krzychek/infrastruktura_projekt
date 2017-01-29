package com.github.krzychek.tcpdumpgraph.graph.model

class Node(
        val id: NodeId
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Node

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id.display
    }

    operator fun component1() = id


}

