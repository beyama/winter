package io.jentz.winter

import io.jentz.winter.internal.DependencyId

interface ProviderScope {
    fun <T> bind(graph: Graph, id: DependencyId, block: Graph.() -> T): () -> T
}

interface FactoryScope {
    fun <A, R> bind(graph: Graph, id: DependencyId, block: Graph.(A) -> R): (A) -> R
}

val prototype = object : ProviderScope {
    override fun <T> bind(graph: Graph, id: DependencyId, block: Graph.() -> T) =
            { graph.evaluateProvider(id, block) }
}

val singleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, id: DependencyId, block: Graph.() -> T) =
            memorize { graph.evaluateProvider(id, block) }
}

val softSingleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, id: DependencyId, block: Graph.() -> T) =
            memorizeSoft { graph.evaluateProvider(id, block) }
}

val weakSingleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, id: DependencyId, block: Graph.() -> T) =
            memorizeWeak { graph.evaluateProvider(id, block) }
}

val prototypeFactory = object : FactoryScope {
    override fun <A, R> bind(graph: Graph, id: DependencyId, block: Graph.(A) -> R) =
            { arg: A -> graph.evaluateFactory(id, arg, block) }
}

val multiton = object : FactoryScope {
    override fun <A, R> bind(graph: Graph, id: DependencyId, block: Graph.(A) -> R) =
            multiton { arg: A -> graph.evaluateFactory(id, arg, block) }
}