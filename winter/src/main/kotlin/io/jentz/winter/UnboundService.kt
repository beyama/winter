package io.jentz.winter

internal interface UnboundService<A, R : Any> {
    val key: DependencyKey
    fun bind(graph: Graph): BoundService<A, R>
}

@PublishedApi
internal class UnboundPrototypeService<T : Any>(
        override val key: DependencyKey,
        val block: Graph.() -> T,
        val postConstruct: (Graph.(T) -> Unit)? = null
) : UnboundService<Unit, T> {

    override fun bind(graph: Graph): BoundService<Unit, T> {
        return BoundPrototypeService(graph, this)
    }
}

@PublishedApi
internal class UnboundSingletonService<T : Any>(
        override val key: DependencyKey,
        val block: Graph.() -> T,
        val postConstruct: (Graph.(T) -> Unit)? = null,
        val dispose: ((T) -> Unit)? = null
) : UnboundService<Unit, T> {

    override fun bind(graph: Graph): BoundService<Unit, T> {
        return BoundSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundWeakSingletonService<T : Any>(
        override val key: DependencyKey,
        val block: Graph.() -> T,
        val postConstruct: (Graph.(T) -> Unit)? = null
) : UnboundService<Unit, T> {

    override fun bind(graph: Graph): BoundService<Unit, T> {
        return BoundWeakSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundSoftSingletonService<T : Any>(
        override val key: DependencyKey,
        val block: Graph.() -> T,
        val postConstruct: (Graph.(T) -> Unit)? = null
) : UnboundService<Unit, T> {

    override fun bind(graph: Graph): BoundService<Unit, T> {
        return BoundSoftSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundFactoryService<A, R : Any>(
        override val key: DependencyKey,
        val block: Graph.(A) -> R,
        val postConstruct: (Graph.(A, R) -> Unit)? = null
) : UnboundService<A, R> {

    override fun bind(graph: Graph): BoundService<A, R> {
        return BoundFactoryService(graph, this)
    }
}

@PublishedApi
internal class UnboundMultitonFactoryService<A, R : Any>(
        override val key: DependencyKey,
        val block: Graph.(A) -> R,
        val postConstruct: (Graph.(A, R) -> Unit)? = null,
        val dispose: ((A, R) -> Unit)? = null
) : UnboundService<A, R> {

    override fun bind(graph: Graph): BoundService<A, R> {
        return BoundMultitonFactoryService(graph, this)
    }
}

@PublishedApi
internal class ConstantService<T : Any>(
        override val key: DependencyKey,
        val value: T
) : UnboundService<Unit, T>, BoundService<Unit, T> {

    override fun bind(graph: Graph): BoundService<Unit, T> = this

    override fun instance(arg: Unit): T = value

    override fun postConstruct(arg: Any, instance: Any) {
    }

    override fun dispose() {
    }
}