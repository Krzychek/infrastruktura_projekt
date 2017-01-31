package com.github.krzychek.tcpdumpgraph.graph

import com.github.krzychek.tcpdumpgraph.graph.model.GraphModel
import com.github.krzychek.tcpdumpgraph.killOnShutdown
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.concurrent.thread

private val executor = Executors.newFixedThreadPool(4)
fun startUpdatingGraphStats(graphModel: GraphModel) = thread(name = "NodeStatsReader") {
    val futures = arrayListOf<CompletableFuture<*>>()
    while (true) {


        graphModel.nodes
                .filter { it.id.isIP }
                .forEach { ipNode ->
                    futures.add(CompletableFuture.supplyAsync(Supplier {
                        ProcessBuilder(listOf("ping", ipNode.id.stringId, "-c", "1")).start()
                                .killOnShutdown()
                                .apply { waitFor() }
                                .inputStream.bufferedReader()
                                .readLines()
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