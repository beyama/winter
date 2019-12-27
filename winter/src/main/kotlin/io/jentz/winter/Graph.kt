package io.jentz.winter

import io.jentz.winter.evaluator.ServiceEvaluator
import io.jentz.winter.evaluator.createServiceEvaluator
import io.jentz.winter.plugin.Plugins

/**
 * The object graph class that retrieves and instantiates dependencies registered in its component.
 *
 * An instance is created by calling [Component.createGraph], [Graph.createSubgraph]
 * or [Graph.openSubgraph].
 */
class Graph internal constructor(
    application: WinterApplication,
    parent: Graph?,
    component: Component,
    onDisposeCallback: OnDisposeCallback?,
    block: ComponentBuilderBlock?
) {

    private sealed class State {

        class Initialized(
            val graph: Graph,
            val component: Component,
            val parent: Graph?,
            val application: WinterApplication,
            val plugins: Plugins,
            val serviceEvaluator: ServiceEvaluator,
            val registry: MutableMap<TypeKey<*>, BoundService<*>> = mutableMapOf(),
            val onDisposeCallback: OnDisposeCallback?
        ) : State() {
            var isDisposing = false

            @Suppress("UNCHECKED_CAST")
            fun <R : Any> serviceOrNull(key: TypeKey<R>): BoundService<R>? =
                registry.getOrPut(key) {
                    component[key]?.bind(graph) ?: return parent?.serviceOrNull(key)
                } as? BoundService<R>

            fun <R : Any> service(key: TypeKey<R>): BoundService<R> = serviceOrNull(key)
                ?: throw EntryNotFoundException(key, "Service with key `$key` does not exist.")

        }

        object Disposed : State()
    }

    private var state: State

    private inline fun <T> fold(ifDisposed: () -> T, ifInitialized: (State.Initialized) -> T): T =
        when (val state = this.state) {
            is State.Disposed -> ifDisposed()
            is State.Initialized -> ifInitialized(state)
        }

    private inline fun <T> synchronizedFold(
        ifDisposed: () -> T,
        ifInitialized: (State.Initialized) -> T
    ): T = synchronized(this) { fold(ifDisposed, ifInitialized) }

    private inline fun <T> map(block: (State.Initialized) -> T): T =
        fold({ throw WinterException("Graph is already disposed.") }, block)

    private inline fun <T> synchronizedMap(block: (State.Initialized) -> T): T =
        synchronizedFold({ throw WinterException("Graph is already disposed.") }, block)

    /**
     * The [WinterApplication] of this graph.
     */
    val application: WinterApplication get() = map { it.application }

    /**
     * The parent [Graph] instance or null if no parent exists.
     */
    val parent: Graph? get() = map { it.parent }

    /**
     * The [Component] instance.
     */
    val component: Component get() = map { it.component }

    /**
     * Indicates if the graph is disposed.
     */
    val isDisposed: Boolean get() = fold({ true }, { false })

    init {
        val plugins = application.plugins

        val baseComponent = if (plugins.isNotEmpty() || block != null) {
            component.derive {
                block?.invoke(this)
                plugins.forEach { it.graphInitializing(parent, this) }
            }
        } else {
            component
        }

        state = State.Initialized(
            graph = this,
            component = baseComponent,
            parent = parent,
            application = application,
            plugins = plugins,
            serviceEvaluator = createServiceEvaluator(
                graph = this,
                component = baseComponent,
                plugins = plugins,
                checkForCyclicDependencies = application.checkForCyclicDependencies
            ),
            onDisposeCallback = onDisposeCallback
        )

        plugins.forEach { it.graphInitialized(this) }

        instanceOrNullByKey(eagerDependenciesKey)?.forEach { key ->
            try {
                instanceByKey(key)
            } catch (e: EntryNotFoundException) {
                throw EntryNotFoundException(
                    key, "BUG: Eager dependency with key `$key` doesn't exist."
                )
            }
        }
    }

    /**
     * Retrieve a non-optional instance of `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return An instance of `R`
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified R : Any> instance(
        qualifier: Any? = null,
        generics: Boolean = false
    ): R = instanceByKey(typeKey(qualifier, generics))

    /**
     * Retrieve a non-optional instance of `R` by [key].
     *
     * @param key The type key of the instance.
     * @return An instance of `R`
     *
     * @throws EntryNotFoundException
     */
    fun <R : Any> instanceByKey(key: TypeKey<R>): R =
        synchronizedMap { it.service(key).instance() }

    /**
     * Retrieve an optional instance of `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return An instance of `R` or null if provider doesn't exist.
     */
    inline fun <reified R : Any> instanceOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ): R? = instanceOrNullByKey(typeKey(qualifier, generics))

    /**
     * Retrieve an optional instance of `R` by [key].
     *
     * @param key The type key of the instance.
     * @return An instance of `R` or null if provider doesn't exist.
     */
    fun <R : Any> instanceOrNullByKey(key: TypeKey<R>): R? =
        synchronizedMap { it.serviceOrNull(key)?.instance() }

    /**
     * Retrieves a non-optional provider function that returns `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The provider function.
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified R : Any> provider(
        qualifier: Any? = null,
        generics: Boolean = false
    ): Provider<R> = providerByKey(typeKey(qualifier, generics))

    /**
     * Retrieves a non-optional provider function by [key] that returns `R`.
     *
     * @param key The type key of the instance.
     * @return The provider function.
     *
     * @throws EntryNotFoundException
     */
    fun <R : Any> providerByKey(key: TypeKey<R>): Provider<R> = synchronizedMap {
        val service = it.service(key)
        return { synchronized(this) { service.instance() } }
    }

    /**
     * Retrieve an optional provider function that returns `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The provider that returns `R` or null if provider doesn't exist.
     */
    inline fun <reified R : Any> providerOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ): Provider<R>? = providerOrNullByKey(typeKey(qualifier, generics))

    /**
     * Retrieve an optional provider function by [key] that returns `R`.
     *
     * @param key The type key of the instance.
     * @return The provider that returns `R` or null if provider doesn't exist.
     */
    fun <R : Any> providerOrNullByKey(key: TypeKey<R>): Provider<R>? =
        synchronizedMap {
            val service = it.serviceOrNull(key) ?: return null
            return { synchronized(this) { service.instance() } }
        }

    /**
     * Retrieve all instances of type `R`.
     *
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return A [Set] of instances of type `R`.
     */
    inline fun <reified R : Any> instancesOfType(generics: Boolean = false): Set<R> =
        instancesOfTypeByKey(typeKeyOfType(generics))

    /**
     * Retrieve all providers of type `T`.
     *
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return A [Set] of [providers][Provider] of type `T`.
     */
    inline fun <reified T : Any> providersOfType(generics: Boolean = false): Set<Provider<T>> =
        providersOfTypeByKey(typeKeyOfType(generics))

    /**
     * Retrieve all instances of type `R` by [key].
     *
     * @param key The type key.
     * @return A [Set] of instances of type `R`.
     */
    fun <R : Any> instancesOfTypeByKey(key: TypeKey<R>): Set<R> {
        require(key.qualifier == TYPE_KEY_OF_TYPE_QUALIFIER) {
            "Type key qualifier must be `$TYPE_KEY_OF_TYPE_QUALIFIER`."
        }
        return keysOfType(key).mapTo(mutableSetOf()) { instanceByKey(it) }
    }

    /**
     * Retrieve all providers for type `T` by [key].
     *
     * @param key The type key.
     * @return A [Set] of [providers][Provider] of type `T`.
     */
    fun <R : Any> providersOfTypeByKey(key: TypeKey<R>): Set<Provider<R>> {
        require(key.qualifier == TYPE_KEY_OF_TYPE_QUALIFIER) {
            "Type key qualifier must be `$TYPE_KEY_OF_TYPE_QUALIFIER`."
        }
        return keysOfType(key).mapTo(mutableSetOf()) { providerByKey(it) }
    }

    private fun <R : Any> keysOfType(key: TypeKey<R>): Set<TypeKey<R>> =
        synchronizedMap { state ->
            @Suppress("UNCHECKED_CAST")
            val service = state.registry.getOrPut(key) {
                val keys = keys().filter { it.typeEquals(key) }.toSet()
                ConstantService(key, keys)
            } as ConstantService<Set<TypeKey<R>>>
            service.value
        }

    private fun keys(): Set<TypeKey<*>> {
        val keys = component.keys()
        return parent?.keys()?.let { keys + it } ?: keys
    }

    internal fun <R : Any> service(key: TypeKey<R>): BoundService<R> =
        synchronizedMap { it.service(key) }

    internal fun <R : Any> serviceOrNull(key: TypeKey<R>): BoundService<R>? =
        synchronizedMap { it.serviceOrNull(key) }

    /**
     * This is called from [BoundService.instance] when a new instance is created.
     * Don't use this method except in custom [BoundService] implementations.
     */
    fun <R : Any> evaluate(service: BoundService<R>): R =
        map { it.serviceEvaluator.evaluate(service) }

    /**
     * Inject members of class [T].
     *
     * @param instance The instance to inject members to.
     *
     * @throws WinterException When no members injector was found.
     */
    fun <T : Any> inject(instance: T): T {
        var injector: MembersInjector<T>? = null
        var cls: Class<*>? = instance.javaClass

        while (cls != null) {
            @Suppress("EmptyCatchBlock")
            try {
                val className = cls.name + "_WinterMembersInjector"
                @Suppress("UNCHECKED_CAST")
                val injectorClass = Class.forName(className) as Class<MembersInjector<T>>
                injector = injectorClass.getConstructor().newInstance()
                break
            } catch (e: Exception) {
            }

            cls = cls.superclass
        }

        if (injector == null) {
            throw WinterException("No members injector found for `${instance.javaClass}`.")
        }

        injector(this, instance)

        return instance
    }

    /**
     * Initialize and return a subgraph by using the subcomponent with [subcomponentQualifier] and
     * this graph as parent.
     *
     * A graph initialized with this method doesn't get disposed when its parent gets disposed
     * but becomes inconsistent.
     *
     * Use it with caution in cases where you need to initialize a lot of short-lived subgraphs that
     * are managed by you e.g. a per request subgraph on a HTTP server that gets created per
     * request and destroyed at the end.
     *
     * @param subcomponentQualifier The subcomponentQualifier of the subcomponent.
     * @param block An optional builder block to register provider on the subcomponent.
     */
    fun createSubgraph(
        subcomponentQualifier: Any,
        block: ComponentBuilderBlock? = null
    ): Graph = synchronizedMap { state ->
        Graph(state.application, this, instance(subcomponentQualifier), null, block)
    }

    /**
     * Initialize and return a subgraph by using the subcomponent with [subcomponentQualifier] and
     * this graph as parent and register it under the [subcomponentQualifier] or when given under
     * [identifier].
     *
     * The resulting graph gets automatically disposed when this graph gets disposed.
     * You can later retrieve the subgraph by calling an instance retrieve method e.g.:
     * ```
     * parent.instance<Graph>(identifier)
     * ```
     *
     * @param subcomponentQualifier The qualifier of the subcomponent.
     * @param identifier An optional identifier to register the subgraph with.
     * @param block An optional builder block to register provider on the subcomponent.
     */
    fun openSubgraph(
        subcomponentQualifier: Any,
        identifier: Any? = null,
        block: ComponentBuilderBlock? = null
    ): Graph = synchronizedMap { state ->
        val name = identifier ?: subcomponentQualifier
        val key = typeKey<Graph>(name)

        if (key in state.registry) {
            throw WinterException(
                "Cannot open subgraph with identifier `$name` because it is already open."
            )
        }

        Graph(
            application = state.application,
            parent = this,
            component = instance(subcomponentQualifier),
            onDisposeCallback = {
                synchronizedFold({}, { state ->
                    if (!state.isDisposing) {
                        state.registry.remove(key)
                    }
                })
            },
            block = block
        ).also {
            state.registry[key] = BoundGraphService(key, it)
        }
    }

    /**
     * Close a subgraph by disposing it and removing it from the registry.
     *
     * @param identifier The identifier it was opened with.
     */
    fun closeSubgraph(identifier: Any) {
        synchronizedMap { state ->
            val key = typeKey<Graph>(identifier)
            val service = state.registry.remove(key) ?: throw WinterException(
                "Subgraph with identifier `$identifier` doesn't exist."
            )
            service.dispose()
        }
    }

    /**
     * Runs [graph dispose plugins][io.jentz.winter.plugin.Plugin.graphDispose] and marks this graph
     * as disposed. All resources get released and every retrieval method will throw an exception
     * if called after disposing.
     *
     * Subsequent calls are ignored.
     */
    fun dispose() {
        synchronizedFold({}) { state ->
            try {
                if (state.isDisposing) return

                state.isDisposing = true

                state.plugins.forEach { it.graphDispose(this) }

                state.registry.values.forEach { boundService -> boundService.dispose() }

                state.onDisposeCallback?.invoke(this)
            } finally {
                this.state = State.Disposed
            }
        }
    }

}
