package com.github.krzychek.tcpdumpgraph.graph.model


class NodeId {

    val isIP: Boolean
    val uniqueId: String
    val display: String

    constructor(stringId: String) {
        this.uniqueId = stringId
        display = stringId
        isIP = true
    }


    constructor(previousNode: NodeId, times: Int, nextNode: NodeId?) {
        val (first, second) = arrayOf(previousNode.uniqueId, nextNode?.uniqueId ?: "THE END").apply { sort() }

        this.uniqueId = "$first -> [$times] -> $second"
        display = "$times unknown"
        isIP = false
    }


    override fun toString(): String = display

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as NodeId

        if (uniqueId != other.uniqueId) return false

        return true
    }

    override fun hashCode(): Int = uniqueId.hashCode()


}