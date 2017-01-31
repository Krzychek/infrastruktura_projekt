package com.github.krzychek.tcpdumpgraph.capture

import com.github.krzychek.tcpdumpgraph.capture.model.TCPDumpCapture
import com.github.krzychek.tcpdumpgraph.killOnShutdown
import com.github.krzychek.tcpdumpgraph.model.Address


class TCPDumpReader : (String) -> TCPDumpCapture? {

    private val localHostIPs by lazy {
        ProcessBuilder("ip", "add", "show").start().killOnShutdown()
                .inputStream.reader().readLines()
                .map { ".*inet (.+)/.*".toRegex().matchEntire(it)?.groupValues?.get(1) }
                .filterNotNull()
    }

    fun isIncomming(str: String) = str in localHostIPs

    private val tcpDumpLineRegex = "(?:IP )?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\.\\d+ ?> ?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}).*length (\\d+)(:?\\D.*)?".toRegex()

    override fun invoke(input: String): TCPDumpCapture? =
            tcpDumpLineRegex.matchEntire(input)?.let {
                val (src, dest, length) = it.destructured
                val incomming = isIncomming(dest)
                TCPDumpCapture(
                        address = Address(if (incomming) src else dest),
                        lenght = length.toInt(),
                        incomming = incomming
                )
            } ?: input.printAndNullify()

    private fun String.printAndNullify(): Nothing? = null.apply {
        println("NOT MATCHED: ${this@printAndNullify}")
    }

    fun readFrom(sequence: Sequence<String>) =
            sequence.map(this)
                    .filterNotNull()
}

