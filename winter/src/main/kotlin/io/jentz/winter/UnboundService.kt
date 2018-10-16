package io.jentz.winter

/**
 * Interface for service entries registered in a [Component].
 *
 * Custom implementations can be added to a [Component] by using [ComponentBuilder.register].
 */
interface UnboundService<A, R : Any> {
    /**
     * The [TypeKey] of the type this service is providing.
     */
    val key: TypeKey

    /**
     * Binds this unbound service a given [graph] and returns a [BoundService].
     */
    fun bind(graph: Graph): BoundService<A, R>
}

@PublishedApi
internal class UnboundPrototypeService<R : Any>(
        override val key: TypeKey,
        val factory: GFactory0<R>,
        val postConstruct: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundPrototypeService(graph, this)
    }
}

@PublishedApi
internal class UnboundSingletonService<R : Any>(
        override val key: TypeKey,
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
        override val key: TypeKey,
        val factory: GFactory0<R>,
        val postConstruct: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundWeakSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundSoftSingletonService<R : Any>(
        override val key: TypeKey,
        val factory: GFactory0<R>,
        val postConstruct: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundSoftSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundFactoryService<A, R : Any>(
        override val key: TypeKey,
        val factory: GFactory1<A, R>,
        val postConstruct: GFactoryCallback2<A, R>?
) : UnboundService<A, R> {

    override fun bind(graph: Graph): BoundService<A, R> {
        return BoundFactoryService(graph, this)
    }
}

@PublishedApi
internal class UnboundMultitonFactoryService<A, R : Any>(
        override val key: TypeKey,
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
        override val key: TypeKey,
        val value: R
) : UnboundService<Unit, R>, BoundService<Unit, R> {

    override val scope: Scope get() = Scope.Prototype

    override fun bind(graph: Graph): BoundService<Unit, R> = this

    override fun instance(argument: Unit): R = value

    override fun postConstruct(arg: Any, instance: Any) {
    }

    override fun dispose() {
    }
}

@PublishedApi
internal class ProviderService<R : Any>(
        override val key: TypeKey,
        val provider: Provider<R>
) : UnboundService<Unit, R>, BoundService<Unit, R> {

    override val scope: Scope get() = Scope.Prototype

    override fun bind(graph: Graph): BoundService<Unit, R> = this

    override fun instance(argument: Unit): R = provider()

    override fun postConstruct(arg: Any, instance: Any) {
    }

    override fun dispose() {
    }
}

@PublishedApi
internal class AliasService(
        private val targetKey: TypeKey,
        private val newKey: TypeKey
) : UnboundService<Any, Any> {

    override val key: TypeKey get() = newKey

    override fun bind(graph: Graph): BoundService<Any, Any> {
        try {
            return graph.service(targetKey)
        } catch (t: Throwable) {
            throw WinterException("Error resolving alias `$newKey` pointing to `$targetKey`.", t)
        }
    }

}
