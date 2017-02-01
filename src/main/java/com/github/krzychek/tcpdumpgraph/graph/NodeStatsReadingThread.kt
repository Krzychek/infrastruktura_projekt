package com.github.krzychek.tcpdumpgraph.graph

import com.github.krzychek.tcpdumpgraph.graph.model.GraphModel
import com.github.krzychek.tcpdumpgraph.killOnShutdown
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier

private val executor = Executors.newFixedThreadPool(4)

class NodeStatsReadingThread(kodein: Kodein) : Thread("NodeStatsReader") {

    private val graphModel: GraphModel = kodein.instance()

    override fun run() {
        val futures = arrayListOf<CompletableFuture<*>>()
        while (true) {
            graphModel.nodes
                    .filter { it.id.isIP }
                    .forEach { ipNode ->
                        futures.add(CompletableFuture.supplyAsync(Supplier {
                            ProcessBuilder(listOf("ping", ipNode.id.uniqueId, "-c", "1")).start()
                                    .killOnShutdown()
                                    .inputStream.bufferedReader().readLines()
                                    .forEach {
                                        ".*time=([\\d.]+).*".toRegex()
                                                .matchEntire(it)
                                                ?.groupValues?.let {
                                            ipNode.rtt.accept(it[1].toDouble())
                                        }
                                    }
                        }, executor))
                    }

            CompletableFuture.allOf(*futures.toTypedArray())
                    .join()

            futures.clear()
        }
    }
}