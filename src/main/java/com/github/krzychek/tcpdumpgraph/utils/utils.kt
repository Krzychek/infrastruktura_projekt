package com.github.krzychek.tcpdumpgraph.utils

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal val NOT_IMPLEMENTED: Exception
    get() = UnsupportedOperationException("not implemented")

fun ScheduledExecutorService.scheduleWithFixedDelayX(period: Long, timeUnit: TimeUnit, command: () -> Unit) {
    this.scheduleWithFixedDelay(command, 0, period, timeUnit)
}


inline fun <T> Iterable<T>.iterateInPairs(operation: (T, T) -> Unit) {
    val iterator = iterator()
    var previous = iterator.next()
    while (iterator.hasNext()) {
        val current = iterator.next()
        operation(previous, current)
        previous = current
    }
}