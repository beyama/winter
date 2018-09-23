package io.jentz.winter

internal interface UnboundService<A, R : Any> {
    val key: DependencyKey
    fun bind(graph: Graph): BoundService<A, R>
}

@PublishedApi
internal class UnboundPrototypeService<R : Any>(
        override val key: DependencyKey,
        val factory: GFactory0<R>,
        val postConstruct: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundPrototypeService(graph, this)
    }
}

@PublishedApi
internal class UnboundSingletonService<R : Any>(
        override val key: DependencyKey,
        val factory: GFactory0<R>,
        val postConstruct: GFactoryCallback1<R>?,
        val dispose: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundWeakSingletonService<R : Any>(
        override val key: DependencyKey,
        val factory: GFactory0<R>,
        val postConstruct: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundWeakSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundSoftSingletonService<R : Any>(
        override val key: DependencyKey,
        val factory: GFactory0<R>,
        val postConstruct: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundSoftSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundFactoryService<A, R : Any>(
        override val key: DependencyKey,
        val factory: GFactory1<A, R>,
        val postConstruct: GFactoryCallback2<A, R>?
) : UnboundService<A, R> {

    override fun bind(graph: Graph): BoundService<A, R> {
        return BoundFactoryService(graph, this)
    }
}

@PublishedApi
internal class UnboundMultitonFactoryService<A, R : Any>(
        override val key: DependencyKey,
        val factory: GFactory1<A, R>,
        val postConstruct: GFactoryCallback2<A, R>?,
        val dispose: GFactoryCallback2<A, R>?
) : UnboundService<A, R> {

    override fun bind(graph: Graph): BoundService<A, R> {
        return BoundMultitonFactoryService(graph, this)
    }
}

@PublishedApi
internal class ConstantService<R : Any>(
        override val key: DependencyKey,
        val value: R
) : UnboundService<Unit, R>, BoundService<Unit, R> {

    override fun bind(graph: Graph): BoundService<Unit, R> = this

    override fun instance(arg: Unit): R = value

    override fun postConstruct(arg: Any, instance: Any) {
    }

    override fun dispose() {
    }
}

@PublishedApi
internal class ProviderService<R : Any>(
        override val key: DependencyKey,
        val provider: Provider<R>
) : UnboundService<Unit, R>, BoundService<Unit, R> {

    override fun bind(graph: Graph): BoundService<Unit, R> = this

    override fun instance(arg: Unit): R = provider()

    override fun postConstruct(arg: Any, instance: Any) {
    }

    override fun dispose() {
    }
}