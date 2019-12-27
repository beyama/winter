package io.jentz.winter

/**
 * Interface for service entries registered in a [Component].
 *
 * Custom implementations can be added to a [Component] by using [ComponentBuilder.register].
 */
interface UnboundService<R : Any> {
    /**
     * The [TypeKey] of the type this service is providing.
     */
    val key: TypeKey<R>

    /**
     * Return true if the bound service requires lifecycle calls to [BoundService.postConstruct]
     * or [BoundService.close] otherwise false.
     */
    val requiresLifecycleCallbacks: Boolean

    /**
     * Binds this unbound service a given [graph] and returns a [BoundService].
     */
    fun bind(graph: Graph): BoundService<R>

}

@PublishedApi
internal class UnboundPrototypeService<R : Any>(
    override val key: TypeKey<R>,
    val factory: GFactory<R>,
    val postConstruct: GFactoryCallback<R>?
) : UnboundService<R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null

    override fun bind(graph: Graph): BoundService<R> {
        return BoundPrototypeService(graph, this)
    }
}

@PublishedApi
internal class UnboundSingletonService<R : Any>(
    override val key: TypeKey<R>,
    val factory: GFactory<R>,
    val postConstruct: GFactoryCallback<R>?,
    val dispose: GFactoryCallback<R>?
) : UnboundService<R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null || dispose != null

    override fun bind(graph: Graph): BoundService<R> {
        return BoundSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundWeakSingletonService<R : Any>(
    override val key: TypeKey<R>,
    val factory: GFactory<R>,
    val postConstruct: GFactoryCallback<R>?
) : UnboundService<R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null

    override fun bind(graph: Graph): BoundService<R> {
        return BoundWeakSingletonService(graph, this)
    }
}

@PublishedApi
internal class UnboundSoftSingletonService<R : Any>(
    override val key: TypeKey<R>,
    val factory: GFactory<R>,
    val postConstruct: GFactoryCallback<R>?
) : UnboundService<R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = postConstruct != null

    override fun bind(graph: Graph): BoundService<R> {
        return BoundSoftSingletonService(graph, this)
    }
}

@PublishedApi
internal class ConstantService<R : Any>(
    override val key: TypeKey<R>,
    val value: R
) : UnboundService<R>, BoundService<R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = false

    override val scope: Scope get() = Scope.Prototype

    override fun bind(graph: Graph): BoundService<R> = this

    override fun instance(): R = value

    override fun newInstance(): R {
        throw AssertionError("BUG: This method should not be called.")
    }

    override fun postConstruct(instance: R) {
        throw AssertionError("BUG: This method should not be called.")
    }

    override fun close() {
    }
}

internal class AliasService<R: Any>(
    private val targetKey: TypeKey<*>,
    private val newKey: TypeKey<R>
) : UnboundService<R> {

    override val requiresLifecycleCallbacks: Boolean get() = false

    override val key: TypeKey<R> get() = newKey

    override fun bind(graph: Graph): BoundService<R> {
        try {
            @Suppress("UNCHECKED_CAST")
            return graph.service(targetKey as TypeKey<R>)
        } catch (t: Throwable) {
            throw WinterException("Error resolving alias `$newKey` pointing to `$targetKey`.", t)
        }
    }

}
