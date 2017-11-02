package io.jentz.winter.internal

import io.jentz.winter.FactoryScope
import io.jentz.winter.Graph
import io.jentz.winter.ProviderScope

sealed class ComponentEntry

class ProviderEntry<out T : Any>(private val scope: ProviderScope, private val block: Graph.() -> T) : ComponentEntry() {
    fun bind(graph: Graph) = scope.bind(graph, block)
}

class FactoryEntry<in A : Any, out R : Any>(private val scope: FactoryScope, private val block: Graph.(A) -> R) : ComponentEntry() {
    fun bind(graph: Graph) = scope.bind(graph, block)
}

class ConstantEntry<out T : Any>(val value: T) : ComponentEntry()