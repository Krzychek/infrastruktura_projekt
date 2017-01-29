package com.github.krzychek.tcpdumpgraph.graph.model


class NodeId {

    val stringId: String
    val display: String

    constructor(stringId: String) {
        this.stringId = stringId
        display = stringId
    }


    constructor(previousNode: NodeId, times: Int, nextNode: NodeId?) {
        var first = previousNode.stringId
        var second = nextNode?.stringId ?: "THE END"
        if (first > second) { // TODO hacky hacky
            val tmp = second
            second = first
            first = tmp
        }
        this.stringId = "$first -> [$times] -> $second"
        display = "$times *"
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