package com.github.krzychek.tcpdumpgraph.capture.model

import com.github.krzychek.tcpdumpgraph.model.Address

data class TCPDumpCapture(
        val address: Address,
        val lenght: Int,
        val incomming: Boolean
)