package com.github.krzychek.tcpdumpgraph.capture

import com.github.krzychek.tcpdumpgraph.model.Address
import com.github.krzychek.tcpdumpgraph.capture.model.TCPDumpCapture


class TCPDumpReader(private val isIncomming: (String) -> Boolean)
    : (String) -> TCPDumpCapture? {

    private val tcpDumpLineRegex = "(?:IP )?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\.\\d+ ?> ?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}).*length ?(\\d+)".toRegex()

    override fun invoke(input: String): TCPDumpCapture? =
            tcpDumpLineRegex.matchEntire(input)?.let {
                val (src, dest, length) = it.destructured
                val incomming = isIncomming(dest)
                TCPDumpCapture(
                        address = Address(name = if (incomming) src else dest),
                        lenght = length.toInt(),
                        incomming = incomming
                )
            }
}

