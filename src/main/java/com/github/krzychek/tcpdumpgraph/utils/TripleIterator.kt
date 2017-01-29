package com.github.krzychek.tcpdumpgraph.utils

private class TripleIterator<out T>(private val iterator: Iterator<T>) : Iterator<Triple<T?, T, T?>> {
    private var previous: T? = null
    private var current: T? = null
    private var next: T? = if (iterator.hasNext()) iterator.next() else null

    private fun roll() {
        previous = current
        current = next
        next = if (iterator.hasNext()) iterator.next() else null
    }

    override fun hasNext(): Boolean = next != null

    override fun next(): Triple<T?, T, T?> {
        roll()
        return Triple(previous, current!!, next)
    }
}

fun <T, K> Iterable<T>.tripleMap(operation: (T?, T, T?) -> K) =
        TripleIterator(iterator()).asSequence().map {
            operation(it.first, it.second, it.third)
        }

