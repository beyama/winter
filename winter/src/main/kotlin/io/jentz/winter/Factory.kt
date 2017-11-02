package io.jentz.winter

interface Factory<out T> {
    fun createInstance(graph: Graph): T
}