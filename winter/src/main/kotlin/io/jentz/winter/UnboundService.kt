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
     * Return true if the bound service requires lifecycle calls to [BoundService.onPostConstruct]
     * or [BoundService.onClose] otherwise false.
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
    val onPostConstruct: GFactoryCallback<R>?
) : UnboundService<R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = onPostConstruct != null

    override fun bind(graph: Graph): BoundService<R> {
        return BoundPrototypeService(graph, this)
    }
}

@PublishedApi
internal class UnboundSingletonService<R : Any>(
    override val key: TypeKey<R>,
    val factory: GFactory<R>,
    val onPostConstruct: GFactoryCallback<R>?,
    val onClose: GFactoryCallback<R>?
) : UnboundService<R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = onPostConstruct != null || onClose != null

    override fun bind(graph: Graph): BoundService<R> {
        return BoundSingletonService(graph, this)
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

    override fun onPostConstruct(instance: R) {
        throw AssertionError("BUG: This method should not be called.")
    }

    override fun onClose() {
    }
}

internal class UnboundAliasService<R: Any>(
    private val targetKey: TypeKey<*>,
    private val newKey: TypeKey<R>
) : UnboundService<R> {

    override val requiresLifecycleCallbacks: Boolean get() = false

    override val key: TypeKey<R> get() = newKey

    override fun bind(graph: Graph): BoundService<R> {
        try {
            @Suppress("UNCHECKED_CAST")
            val targetService = graph.service(targetKey as TypeKey<R>)
            return BoundAliasService(newKey, targetService)
        } catch (t: Throwable) {
            throw WinterException("Error resolving alias `$newKey` pointing to `$targetKey`.", t)
        }
    }

}

internal abstract class OfTypeService<T: Any, R: Any>(
    override val key: TypeKey<R>,
    val typeOfKey: TypeKey<T>
) : UnboundService<R> {

    override val requiresLifecycleCallbacks: Boolean
        get() = false

}

@PublishedApi
internal class SetOfTypeService<T: Any>(
    key: TypeKey<Set<T>>,
    typeOfKey: TypeKey<T>
) : OfTypeService<T, Set<T>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Set<T>> =
        BoundSetOfTypeService(graph, this)

}

@PublishedApi
internal class SetOfProvidersForTypeService<T: Any>(
    key: TypeKey<Set<Provider<T>>>,
    typeOfKey: TypeKey<T>
) : OfTypeService<T, Set<Provider<T>>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Set<Provider<T>>> =
        BoundSetOfProvidersForTypeService(graph, this)

}

@PublishedApi
internal class MapOfTypeService<T: Any>(
    key: TypeKey<Map<Any, T>>,
    typeOfKey: TypeKey<T>,
    val defaultKey: Any
) : OfTypeService<T, Map<Any, T>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Map<Any, T>> =
        BoundMapOfTypeService(graph, this)

}

@PublishedApi
internal class MapOfProvidersForTypeService<T: Any>(
    key: TypeKey<Map<Any, Provider<T>>>,
    typeOfKey: TypeKey<T>,
    val defaultKey: Any
) : OfTypeService<T, Map<Any, Provider<T>>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Map<Any, Provider<T>>> =
        BoundMapOfProvidersForTypeService(graph, this)

}
