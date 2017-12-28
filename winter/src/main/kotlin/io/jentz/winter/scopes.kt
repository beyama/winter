package io.jentz.winter

interface ProviderScope {
    fun <T> bind(graph: Graph, unboundProvider: UnboundProvider<T>): Provider<T>
}

interface FactoryScope {
    fun <A, R> bind(graph: Graph, unboundFactory: UnboundFactory<A, R>): Provider<(A) -> R>
}

val prototype = object : ProviderScope {
    override fun <T> bind(graph: Graph, unboundProvider: UnboundProvider<T>) = { unboundProvider(graph) }
}

val singleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, unboundProvider: UnboundProvider<T>) = memorize { unboundProvider(graph) }
}

val softSingleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, unboundProvider: UnboundProvider<T>) = memorizeSoft { unboundProvider(graph) }
}

val weakSingleton = object : ProviderScope {
    override fun <T> bind(graph: Graph, unboundProvider: UnboundProvider<T>) = memorizeWeak { unboundProvider(graph) }
}

val prototypeFactory = object : FactoryScope {
    override fun <A, R> bind(graph: Graph, unboundFactory: UnboundFactory<A, R>): Provider<(A) -> R> {
        val bound = { arg: A -> unboundFactory(graph, arg) }
        return { bound }
    }
}

val multiton = object : FactoryScope {
    override fun <A, R> bind(graph: Graph, unboundFactory: UnboundFactory<A, R>): Provider<(A) -> R> {
        val bound = io.jentz.winter.multiton { arg: A -> unboundFactory(graph, arg) }
        return { bound }
    }
}