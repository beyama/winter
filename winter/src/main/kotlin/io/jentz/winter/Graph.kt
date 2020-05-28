package io.jentz.winter

import io.jentz.winter.delegate.DelegateNotifier
import io.jentz.winter.inject.MembersInjector
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
    onCloseCallback: OnCloseCallback?,
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
            val onCloseCallback: OnCloseCallback?
        ) : State() {
            val registry: MutableMap<TypeKey<*>, BoundService<*>> = hashMapOf()

            var isClosing = false

            init {
                val selfKey = typeKey<Graph>()
                registry[selfKey] = ConstantService(selfKey, graph)
            }

            @Suppress("UNCHECKED_CAST")
            fun <R : Any> serviceOrNull(key: TypeKey<R>): BoundService<R>? =
                registry.getOrPut(key) {
                    component[key]?.bind(graph) ?: return parent?.serviceOrNull(key)
                } as? BoundService<R>

            fun <R : Any> service(key: TypeKey<R>): BoundService<R> = serviceOrNull(key)
                ?: throw EntryNotFoundException(key, "Service with key `$key` does not exist.")

        }

        object Closed : State()
    }

    private var state: State

    private inline fun <T> fold(ifClosed: () -> T, ifInitialized: (State.Initialized) -> T): T =
        when (val state = this.state) {
            is State.Closed -> ifClosed()
            is State.Initialized -> ifInitialized(state)
        }

    private inline fun <T> synchronizedFold(
        ifClosed: () -> T,
        ifInitialized: (State.Initialized) -> T
    ): T = synchronized(this) { fold(ifClosed, ifInitialized) }

    private inline fun <T> map(block: (State.Initialized) -> T): T =
        fold({ throw WinterException("Graph is already closed.") }, block)

    private inline fun <T> synchronizedMap(block: (State.Initialized) -> T): T =
        synchronizedFold({ throw WinterException("Graph is already closed.") }, block)

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
     * Indicates if the graph is closed.
     */
    val isClosed: Boolean get() = fold({ true }, { false })

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
            serviceEvaluator = ServiceEvaluator(this, plugins),
            onCloseCallback = onCloseCallback
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
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return An instance of `R`
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified R : Any> instance(
        qualifier: Any? = null,
        generics: Boolean = false,
        noinline block: ComponentBuilderBlock? = null
    ): R = instanceByKey(typeKey(qualifier, generics), block)

    /**
     * Retrieve a non-optional instance of `R` by [key].
     *
     * @param key The type key of the instance.
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return An instance of `R`
     *
     * @throws EntryNotFoundException
     */
    fun <R : Any> instanceByKey(key: TypeKey<R>, block: ComponentBuilderBlock? = null): R =
        service(key).instance(block)

    /**
     * Retrieve an optional instance of `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return An instance of `R` or null if provider doesn't exist.
     */
    inline fun <reified R : Any> instanceOrNull(
        qualifier: Any? = null,
        generics: Boolean = false,
        noinline block: ComponentBuilderBlock? = null
    ): R? = instanceOrNullByKey(typeKey(qualifier, generics), block)

    /**
     * Retrieve an optional instance of `R` by [key].
     *
     * @param key The type key of the instance.
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return An instance of `R` or null if provider doesn't exist.
     */
    fun <R : Any> instanceOrNullByKey(key: TypeKey<R>, block: ComponentBuilderBlock? = null): R? =
        serviceOrNull(key)?.instance(block)

    /**
     * Retrieves a non-optional provider function that returns `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The provider function.
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified R : Any> provider(
        qualifier: Any? = null,
        generics: Boolean = false,
        noinline block: ComponentBuilderBlock? = null
    ): Provider<R> = providerByKey(typeKey(qualifier, generics), block)

    /**
     * Retrieves a non-optional provider function by [key] that returns `R`.
     *
     * @param key The type key of the instance.
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The provider function.
     *
     * @throws EntryNotFoundException
     */
    fun <R : Any> providerByKey(
        key: TypeKey<R>,
        block: ComponentBuilderBlock? = null
    ): Provider<R> {
        val service = service(key)
        return { service.instance(block) }
    }

    /**
     * Retrieve an optional provider function that returns `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The provider that returns `R` or null if provider doesn't exist.
     */
    inline fun <reified R : Any> providerOrNull(
        qualifier: Any? = null,
        generics: Boolean = false,
        noinline block: ComponentBuilderBlock? = null
    ): Provider<R>? = providerOrNullByKey(typeKey(qualifier, generics), block)

    /**
     * Retrieve an optional provider function by [key] that returns `R`.
     *
     * @param key The type key of the instance.
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The provider that returns `R` or null if provider doesn't exist.
     */
    fun <R : Any> providerOrNullByKey(
        key: TypeKey<R>,
        block: ComponentBuilderBlock? = null
    ): Provider<R>? {
        val service = serviceOrNull(key) ?: return null
        return { service.instance(block) }
    }

    /**
     * Returns a set of all [keys][TypeKey] registered on the backing [Component] and all the
     * ancestor components.
     *
     * This is used internally and may be useful for debugging and testing.
     */
    fun keys(): Set<TypeKey<*>> {
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
    fun <R : Any> evaluate(service: BoundService<R>, block: ComponentBuilderBlock?): R =
        synchronizedMap {
            if (block == null) {
                it.serviceEvaluator.evaluate(service, this)
            } else {
                val graph = derive(block)
                try {
                    it.serviceEvaluator.evaluate(service, graph)
                } finally {
                    graph.close()
                }
            }
        }

    private fun derive(block: ComponentBuilderBlock): Graph = map {
        Graph(it.application, this, component("_DERIVED_", block), null, null)
    }

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

        DelegateNotifier.notify(instance, this)

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

        injector?.inject(this, instance)

        return instance
    }

    /**
     * Initialize and return a subgraph by using the subcomponent with [subcomponentQualifier] and
     * this graph as parent.
     *
     * A graph initialized with this method doesn't get closed when its parent gets closed
     * but becomes inconsistent.
     *
     * Use it with caution in cases where you need to initialize a lot of short-lived subgraphs that
     * are managed by you e.g. a per request subgraph on a HTTP server that gets created per
     * request and closed at the end.
     *
     * @param subcomponentQualifier The subcomponentQualifier of the subcomponent.
     * @param block An optional builder block to derive the subcomponent with.
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
     * The resulting graph gets automatically closed when this graph gets closed.
     * You can later retrieve the subgraph by calling an instance retrieve method e.g.:
     * ```
     * parent.instance<Graph>(identifier)
     * ```
     *
     * @param subcomponentQualifier The qualifier of the subcomponent.
     * @param identifier An optional identifier to register the subgraph with.
     * @param block An optional builder block to derive the subcomponent with.
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
            onCloseCallback = {
                if (state.isClosing) return@Graph
                synchronizedFold({}, { state ->
                    if (!state.isClosing) {
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
     * Close a subgraph and remove it from the registry.
     *
     * @param identifier The identifier it was opened with.
     */
    fun closeSubgraph(identifier: Any) {
        synchronizedMap { state ->
            val key = typeKey<Graph>(identifier)
            val service = state.registry.remove(key) ?: throw WinterException(
                "Subgraph with identifier `$identifier` doesn't exist."
            )
            service.onClose()
        }
    }

    /**
     * Close a subgraph and remove it from the registry if it is open.
     *
     * @param identifier The identifier it was opened with.
     */
    fun closeSubgraphIfOpen(identifier: Any) {
        synchronizedMap { it.registry.remove(typeKey<Graph>(identifier))?.onClose() }
    }

    /**
     * Get a subgraph by [identifier].
     *
     * Alias for `instance<Graph>(identifier)`
     *
     * @param identifier The identifier it was opened with.
     */
    fun getSubgraph(identifier: Any): Graph = instance(identifier)

    /**
     * Get an optional subgraph by [identifier].
     *
     * Alias for `instanceOrNull<Graph>(identifier)`
     *
     * @param identifier The identifier it was opened with.
     */
    fun getSubgraphOrNull(identifier: Any): Graph? = instanceOrNull(identifier)

    /**
     * Get a subgraph by [identifier] if present or open and return it.
     *
     * @param subcomponentQualifier The qualifier of the subcomponent.
     * @param identifier An optional qualifier for the graph.
     * @param block An optional builder block to derive the subcomponent with.
     */
    fun getOrOpenSubgraph(
        subcomponentQualifier: Any,
        identifier: Any? = null,
        block: ComponentBuilderBlock? = null
    ): Graph = synchronizedMap {
        val qualifier = identifier ?: subcomponentQualifier
        instanceOrNull(qualifier) ?: openSubgraph(subcomponentQualifier, identifier, block)
    }

    /**
     * Runs [graph close plugins][io.jentz.winter.plugin.Plugin.graphClose] and marks this graph
     * as closed. All resources get released and every retrieval method will throw an exception
     * if called after closing.
     *
     * Subsequent calls are ignored.
     */
    fun close() {
        synchronizedFold({}) { state ->
            try {
                if (state.isClosing) return

                state.isClosing = true

                state.plugins.forEach { it.graphClose(this) }

                state.registry.values.forEach { boundService -> boundService.onClose() }

                state.onCloseCallback?.invoke(this)
            } finally {
                this.state = State.Closed
            }
        }
    }

}
