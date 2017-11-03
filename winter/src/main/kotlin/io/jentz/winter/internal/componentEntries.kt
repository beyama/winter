package io.jentz.winter.internal

import io.jentz.winter.FactoryScope
import io.jentz.winter.Graph
import io.jentz.winter.ProviderScope

sealed class ComponentEntry<out T> {
    abstract fun bind(graph: Graph, id: DependencyId): () -> T
}

class ProviderEntry<out T : Any>(private val scope: ProviderScope, private val block: Graph.() -> T) : ComponentEntry<T>() {
    override fun bind(graph: Graph, id: DependencyId) = scope.bind(graph, id, block)
}

class FactoryEntry<in A : Any, out R : Any>(private val scope: FactoryScope, private val block: Graph.(A) -> R) : ComponentEntry<(A) -> R>() {
    override fun bind(graph: Graph, id: DependencyId): () -> (A) -> R {
        val bound = scope.bind(graph, id, block)
        return { bound }
    }
}

class ConstantEntry<out T : Any>(val value: T) : ComponentEntry<T>() {
    override fun bind(graph: Graph, id: DependencyId): () -> T = { value }
}