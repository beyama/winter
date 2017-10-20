package io.jentz.winter

interface ProviderScope {
    fun <T> bind(graph: Graph, block: Graph.() -> T): () -> T
}

interface FactoryScope {
    fun <A, R> bind(graph: Graph, block: Graph.(A) -> R): (A) -> R
}

val prototype = object : ProviderScope {
    override fun <T> bind(graph: Graph, block: Graph.() -> T) = { graph.block() }
}

val singleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, block: Graph.() -> T) = memorize { graph.block() }
}

val softSingleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, block: Graph.() -> T) = memorizeSoft { graph.block() }
}

val weakSingleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, block: Graph.() -> T) = memorizeWeak { graph.block() }
}

val prototypeFactory = object : FactoryScope {
    override fun <A, R> bind(graph: Graph, block: Graph.(A) -> R) = { arg: A -> graph.block(arg) }
}

val multiton = object : FactoryScope {
    override fun <A, R> bind(graph: Graph, block: Graph.(A) -> R) = multiton { arg: A -> graph.block(arg) }
}