package io.jentz.winter.internal

import io.jentz.winter.*

sealed class ComponentEntry<out T : Any> {
    abstract fun bind(graph: Graph): Provider<T>
}

class ProviderEntry<out T : Any>(private val scope: ProviderScope,
                                 private val unboundProvider: UnboundProvider<T>) : ComponentEntry<T>() {
    override fun bind(graph: Graph) = scope.bind(graph, unboundProvider)
}

class FactoryEntry<in A : Any, out R : Any>(private val scope: FactoryScope,
                                            private val unboundFactory: UnboundFactory<A, R>) : ComponentEntry<(A) -> R>() {
    override fun bind(graph: Graph) = scope.bind(graph, unboundFactory)
}

class ConstantEntry<out T : Any>(val value: T) : ComponentEntry<T>() {
    override fun bind(graph: Graph): () -> T = { value }
}

class MembersInjectorEntry<in T : Any>(private val block: () -> MembersInjector<T>) : ComponentEntry<MembersInjector<T>>() {
    override fun bind(graph: Graph) = block
}