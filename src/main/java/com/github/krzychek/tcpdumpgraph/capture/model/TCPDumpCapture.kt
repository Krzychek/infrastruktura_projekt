package com.github.krzychek.tcpdumpgraph.capture.model

data class TCPDumpCapture(
        val address: Address,
        val lenght: Int,
        val incomming: Boolean
)