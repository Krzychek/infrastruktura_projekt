package com.github.krzychek.tcpdumpgraph.graph.model


class NodeId {

    val isIP: Boolean
    val stringId: String
    val display: String

    constructor(stringId: String) {
        this.stringId = stringId
        display = stringId
        isIP = true
    }


    constructor(previousNode: NodeId, times: Int, nextNode: NodeId?) {
        val (first, second) = arrayOf(previousNode.stringId, nextNode?.stringId ?: "THE END").apply { sort() }

        this.stringId = "$first -> [$times] -> $second"
        display = "$times uknown"
        isIP = false
    }


    override fun toString(): String = display

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as NodeId

        if (stringId != other.stringId) return false

        return true
    }

    override fun hashCode(): Int = stringId.hashCode()


}