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
    val key: TypeKey<A, R>

    /**
     * Return true if the bound service requires lifecycle calls to [BoundService.postConstruct]
     * or [BoundService.dispose] otherwise false.
     */
    val requiresLifecycleCallbacks: Boolean

    /**
     * Binds this unbound service a given [graph] and returns a [BoundService].
     */
    fun bind(graph: Graph): BoundService<A, R>

}

@PublishedApi
internal class UnboundPrototypeService<R : Any>(
    override val key: TypeKey<Unit, R>,
    val factory: GFactory0<R>,
    val postConstruct: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundPrototypeService(graph, this)
    }
}

@PublishedApi
internal class UnboundSingletonService<R : Any>(
    override val key: TypeKey<Unit, R>,
    val factory: GFactory0<R>,
    val postConstruct: GFactoryCallback1<R>?,
    val dispose: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null || dispose != null

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundWeakSingletonService<R : Any>(
    override val key: TypeKey<Unit, R>,
    val factory: GFactory0<R>,
    val postConstruct: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundWeakSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundSoftSingletonService<R : Any>(
    override val key: TypeKey<Unit, R>,
    val factory: GFactory0<R>,
    val postConstruct: GFactoryCallback1<R>?
) : UnboundService<Unit, R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null

    override fun bind(graph: Graph): BoundService<Unit, R> {
        return BoundSoftSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundFactoryService<A, R : Any>(
    override val key: TypeKey<A, R>,
    val factory: GFactory1<A, R>,
    val postConstruct: GFactoryCallback2<A, R>?
) : UnboundService<A, R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null

    override fun bind(graph: Graph): BoundService<A, R> {
        return BoundFactoryService(graph, this)
    }
}

@PublishedApi
internal class UnboundMultitonFactoryService<A, R : Any>(
    override val key: TypeKey<A, R>,
    val factory: GFactory1<A, R>,
    val postConstruct: GFactoryCallback2<A, R>?,
    val dispose: GFactoryCallback2<A, R>?
) : UnboundService<A, R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null || dispose != null

    override fun bind(graph: Graph): BoundService<A, R> {
        return BoundMultitonFactoryService(graph, this)
    }
}

@PublishedApi
internal class ConstantService<R : Any>(
    override val key: TypeKey<Unit, R>,
    val value: R
) : UnboundService<Unit, R>, BoundService<Unit, R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = false

    override val scope: Scope get() = Scope.Prototype

    override fun bind(graph: Graph): BoundService<Unit, R> = this

    override fun instance(argument: Unit): R = value

    override fun newInstance(argument: Unit): R {
        throw AssertionError("BUG: This method should not be called.")
    }

    override fun postConstruct(argument: Unit, instance: R) {
        throw AssertionError("BUG: This method should not be called.")
    }

    override fun dispose() {
    }
}

@PublishedApi
internal class ProviderService<R : Any>(
    override val key: TypeKey<Unit, R>,
    val provider: Provider<R>
) : UnboundService<Unit, R>, BoundService<Unit, R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = false

    override val scope: Scope get() = Scope.Prototype

    override fun bind(graph: Graph): BoundService<Unit, R> = this

    override fun instance(argument: Unit): R = provider()

    override fun newInstance(argument: Unit): R {
        throw AssertionError("BUG: This method should not be called.")
    }

    override fun postConstruct(argument: Unit, instance: R) {
        throw AssertionError("BUG: This method should not be called.")
    }

    override fun dispose() {
    }
}

internal class AliasService<A0, R0: Any>(
    private val targetKey: TypeKey<*, *>,
    private val newKey: TypeKey<A0, R0>
) : UnboundService<A0, R0> {

    override val requiresLifecycleCallbacks: Boolean get() = false

    override val key: TypeKey<A0, R0> get() = newKey

    override fun bind(graph: Graph): BoundService<A0, R0> {
        try {
            @Suppress("UNCHECKED_CAST")
            return graph.service(targetKey as TypeKey<A0, R0>)
        } catch (t: Throwable) {
            throw WinterException("Error resolving alias `$newKey` pointing to `$targetKey`.", t)
        }
    }

}
