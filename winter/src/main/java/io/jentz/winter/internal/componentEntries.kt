package io.jentz.winter.internal

import io.jentz.winter.FactoryScope
import io.jentz.winter.Graph
import io.jentz.winter.ProviderScope

sealed class ComponentEntry

class Provider<out T>(private val scope: ProviderScope, private val block: Graph.() -> T) : ComponentEntry() {
    fun bind(graph: Graph) = scope.bind(graph, block)
}

class Factory<in A, out R>(private val scope: FactoryScope, private val block: Graph.(A) -> R) : ComponentEntry() {
    fun bind(graph: Graph) = scope.bind(graph, block)
}

class Constant<out T>(val value: T) : ComponentEntry()