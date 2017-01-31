package com.github.krzychek.tcpdumpgraph.graph

import com.github.krzychek.tcpdumpgraph.graph.model.GraphModel
import com.github.krzychek.tcpdumpgraph.killOnShutdown
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.concurrent.thread

private val executor = Executors.newFixedThreadPool(4)
fun startUpdatingGraphStats(kodein: Kodein) = thread(name = "NodeStatsReader") {
    val graphModel: GraphModel = kodein.instance()
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