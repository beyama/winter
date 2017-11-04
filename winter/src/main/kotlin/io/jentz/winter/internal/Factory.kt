package io.jentz.winter.internal

import io.jentz.winter.Graph

interface Factory<out T> {
    fun createInstance(graph: Graph): T
}