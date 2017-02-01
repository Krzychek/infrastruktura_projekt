package com.github.krzychek.tcpdumpgraph.utils

import java.awt.Point
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

fun ScheduledExecutorService.kScheduleWithFixedDelay(delay: Long = 0, period: Long, timeUnit: TimeUnit, command: () -> Unit) {
    this.scheduleWithFixedDelay(command, delay, period, timeUnit)
}

operator fun Point.component1() = x
operator fun Point.component2() = y