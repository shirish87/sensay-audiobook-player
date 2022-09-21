package com.dotslashlabs.sensay.util

fun <E> Collection<E>.joinWith(addElement: E): Collection<E> {
    val lastIndex = this.size - 1

    return foldIndexed(mutableListOf()) { idx, acc, item ->
        acc.add(item)

        if (idx < lastIndex) {
            acc.add(addElement)
        }
        acc
    }
}
