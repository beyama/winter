package io.jentz.winter.internal

import io.jentz.winter.FactoryScope
import io.jentz.winter.Graph
import io.jentz.winter.ProviderScope

sealed class ComponentEntry<out T> {
    abstract fun bind(graph: Graph, key: DependencyKey): () -> T
}

class ProviderEntry<out T : Any>(private val scope: ProviderScope, private val block: Graph.() -> T) : ComponentEntry<T>() {
    override fun bind(graph: Graph, key: DependencyKey) = scope.bind(graph, key, block)
}

class FactoryEntry<in A : Any, out R : Any>(private val scope: FactoryScope, private val block: Graph.(A) -> R) : ComponentEntry<(A) -> R>() {
    override fun bind(graph: Graph, key: DependencyKey): () -> (A) -> R {
        val bound = scope.bind(graph, key, block)
        return { bound }
    }
}

class ConstantEntry<out T : Any>(val value: T) : ComponentEntry<T>() {
    override fun bind(graph: Graph, key: DependencyKey): () -> T = { value }
}

class MembersInjectorEntry<in T : Any>(private val block: () -> MembersInjector<T>) : ComponentEntry<MembersInjector<T>>() {
    override fun bind(graph: Graph, key: DependencyKey) = block
}