package io.jentz.winter.internal

import io.jentz.winter.*

internal sealed class ComponentEntry<out T : Any> {
    abstract fun bind(graph: Graph): Provider<T>
}

internal class UnboundProviderEntry<out T : Any>(private val scope: ProviderScope,
                                                 private val unboundProvider: UnboundProvider<T>) : ComponentEntry<T>() {
    override fun bind(graph: Graph) = scope.bind(graph, unboundProvider)
}

internal class FactoryEntry<in A : Any, out R : Any>(private val scope: FactoryScope,
                                                     private val unboundFactory: UnboundFactory<A, R>) : ComponentEntry<(A) -> R>() {
    override fun bind(graph: Graph) = scope.bind(graph, unboundFactory)
}

internal class ConstantEntry<out T : Any>(val value: T) : ComponentEntry<T>() {
    override fun bind(graph: Graph): () -> T = { value }
}

internal class ProviderEntry<out T : Any>(val provider: Provider<T>) : ComponentEntry<T>() {
    override fun bind(graph: Graph) = provider
}