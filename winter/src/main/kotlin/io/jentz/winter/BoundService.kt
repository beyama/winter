package io.jentz.winter

/**
 * Interface for bound service entries in a [Graph].
 */
abstract class BoundService<R : Any> {

    protected abstract val unboundService: UnboundService<R>

    /**
     * Internal marker if dependency resolution for this is pending to detect cyclic dependencies.
     */
    @JvmField internal var isPending: Boolean = false

    /**
     * The [TypeKey] of the type this service is providing.
     */
    open val key: TypeKey<R> get() = unboundService.key

    /**
     * A scope that is unique for this type of service e.g. Scope("myCustomScope").
     */
    open val scope: Scope get() = unboundService.scope

    /**
     * Return true if the bound service requires a call to [BoundService.onPostConstruct]
     * after constructing.
     */
    open val requiresPostConstructCallback: Boolean get() = unboundService.requiresPostConstructCallback

    /**
     * This is called every time an instance is requested from the [Graph].
     * Calls to this might not be synchronized.
     *
     * If this service has to create a new instance to satisfy this request it must do the
     * initialization in [newInstance] by calling [Graph.evaluate].
     *
     * @param block An optional builder block to derive the graph to pass runtime values to the
     * factory. This has to be passed to [Graph.evaluate] and if present will result in a extended
     * graph passed to [newInstance].
     *
     * @return An instance of type `R`.
     */
    abstract fun instance(block: ComponentBuilderBlock? = null): R

    /**
     * This is called when this instance is passed to [Graph.evaluate] to create a new instance.
     *
     * If you want to memorize the value this is the place to do it.
     *
     * @param graph The graph that must be used to call this services factory. This is either the
     * [Graph] this [BoundService] was bound to or an extended version if a builder block was passed
     * to [instance].
     *
     * @return The new instance of type `R`.
     */
    abstract fun newInstance(graph: Graph): R

    /**
     * This is called after a new instance was created but not until the complete dependency request
     * is finished.
     *
     * For example:
     * ```
     * graph {
     *   singleton { Parent(child = instance()) }
     *   singleton { Child() }
     * }
     * ```
     * When Parent is requested, Child has to be created but the [onPostConstruct] method of the
     * Child service is called after Parent is initialized. This way we can resolve cyclic
     * dependencies in post-construct callbacks.
     *
     */
    open fun onPostConstruct(instance: R) {
    }

    /**
     * This is called for each [BoundService] in a [Graph] when [Graph.close] is called.
     */
    open fun onClose() {
    }

}

internal class BoundPrototypeService<R : Any>(
    private val graph: Graph,
    override val unboundService: UnboundPrototypeService<R>
) : BoundService<R>() {

    override fun instance(block: ComponentBuilderBlock?): R =
        graph.evaluate(this, block)

    override fun newInstance(graph: Graph): R =
        unboundService.factory(graph)

    override fun onPostConstruct(instance: R) {
        unboundService.onPostConstruct?.invoke(graph, instance)
    }

}

internal class BoundSingletonService<R : Any>(
    private val graph: Graph,
    override val unboundService: UnboundSingletonService<R>
) : BoundService<R>() {

    @Volatile private var _value = UNINITIALIZED_VALUE

    @Suppress("UNCHECKED_CAST")
    override fun instance(block: ComponentBuilderBlock?): R {
        val v1 = _value
        if (v1 !== UNINITIALIZED_VALUE) {
            return v1 as R
        }
        synchronized(this) {
            val v2 = _value
            if (v2 !== UNINITIALIZED_VALUE) {
                return v2 as R
            }

            return graph.evaluate(this, block)
        }
    }

    override fun newInstance(graph: Graph): R =
        unboundService.factory(graph).also { _value = it }

    override fun onPostConstruct(instance: R) {
        unboundService.onPostConstruct?.invoke(graph, instance)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onClose() {
        val onClose = unboundService.onClose ?: return
        val instance = _value.takeUnless { it === UNINITIALIZED_VALUE } ?: return
        onClose(graph, instance as R)
    }

}

internal class BoundAliasService<R : Any>(
    override val unboundService: UnboundAliasService<R>,
    private val targetService: BoundService<R>
) : BoundService<R>() {

    override fun instance(block: ComponentBuilderBlock?): R = targetService.instance(block)

    override fun newInstance(graph: Graph): R {
        throw AssertionError("BUG: This method should not be called.")
    }

}

internal class BoundGraphService(
    override val key: TypeKey<Graph>,
    private val graph: Graph
) : BoundService<Graph>(), UnboundService<Graph> {

    override val unboundService: UnboundService<Graph> get() = this

    override val requiresPostConstructCallback: Boolean
        get() = false

    override val scope: Scope
        get() = Scope.Singleton

    override fun bind(graph: Graph): BoundService<Graph> = this

    override fun instance(block: ComponentBuilderBlock?): Graph = graph

    override fun newInstance(graph: Graph): Graph {
        throw AssertionError("BUG: This method should not be called.")
    }

    override fun onClose() {
        graph.close()
    }
}

internal abstract class BoundOfTypeService<T : Any, R : Any>(
    protected val graph: Graph
) : BoundService<R>() {

    abstract override val unboundService: OfTypeService<T, R>

    protected val keys: Set<TypeKey<T>> by lazy {
        val typeOfKey = unboundService.typeOfKey
        @Suppress("UNCHECKED_CAST")
        graph.keys().filterTo(mutableSetOf()) { it.typeEquals(typeOfKey) } as Set<TypeKey<T>>
    }

    override fun instance(block: ComponentBuilderBlock?): R =
        graph.evaluate(this, null)

}

internal class BoundSetOfTypeService<T : Any>(
    graph: Graph,
    override val unboundService: SetOfTypeService<T>
) : BoundOfTypeService<T, Set<T>>(graph) {

    override fun newInstance(graph: Graph): Set<T> =
        keys.mapTo(LinkedHashSet(keys.size)) { graph.instanceByKey(it) }

}

internal class BoundSetOfProvidersForTypeService<T : Any>(
    graph: Graph,
    override val unboundService: SetOfProvidersForTypeService<T>
) : BoundOfTypeService<T, Set<Provider<T>>>(graph) {

    override fun newInstance(graph: Graph): Set<Provider<T>> =
        keys.mapTo(LinkedHashSet(keys.size)) { graph.providerByKey(it) }

}

internal class BoundMapOfTypeService<T : Any>(
    graph: Graph,
    override val unboundService: MapOfTypeService<T>
) : BoundOfTypeService<T, Map<Any, T>>(graph) {

    override fun newInstance(graph: Graph): Map<Any, T> =
        keys.associateByTo(HashMap(keys.size), {
            it.qualifier ?: unboundService.defaultKey
        }, {
            graph.instanceByKey(it)
        })

}

internal class BoundMapOfProvidersForTypeService<T : Any>(
    graph: Graph,
    override val unboundService: MapOfProvidersForTypeService<T>
) : BoundOfTypeService<T, Map<Any, Provider<T>>>(graph) {

    override fun newInstance(graph: Graph): Map<Any, Provider<T>> =
        keys.associateByTo(HashMap(keys.size), {
            it.qualifier ?: unboundService.defaultKey
        }, {
            graph.providerByKey(it)
        })

}
