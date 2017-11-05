package io.jentz.winter

import io.jentz.winter.internal.DependencyKey

interface ProviderScope {
    fun <T> bind(graph: Graph, key: DependencyKey, block: Graph.() -> T): () -> T
}

interface FactoryScope {
    fun <A, R> bind(graph: Graph, key: DependencyKey, block: Graph.(A) -> R): (A) -> R
}

val prototype = object : ProviderScope {
    override fun <T> bind(graph: Graph, key: DependencyKey, block: Graph.() -> T) =
            { graph.evaluateProvider(key, block) }
}

val singleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, key: DependencyKey, block: Graph.() -> T) =
            memorize { graph.evaluateProvider(key, block) }
}

val softSingleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, key: DependencyKey, block: Graph.() -> T) =
            memorizeSoft { graph.evaluateProvider(key, block) }
}

val weakSingleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, key: DependencyKey, block: Graph.() -> T) =
            memorizeWeak { graph.evaluateProvider(key, block) }
}

val prototypeFactory = object : FactoryScope {
    override fun <A, R> bind(graph: Graph, key: DependencyKey, block: Graph.(A) -> R) =
            { arg: A -> graph.evaluateFactory(key, arg, block) }
}

val multiton = object : FactoryScope {
    override fun <A, R> bind(graph: Graph, key: DependencyKey, block: Graph.(A) -> R) =
            multiton { arg: A -> graph.evaluateFactory(key, arg, block) }
}