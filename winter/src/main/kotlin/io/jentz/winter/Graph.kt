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
    // only set for graphs that are managed (opened) by the parent graph
    private val identifier: Any?,
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
            val registry: MutableMap<TypeKey, BoundService<*, *>> = mutableMapOf()
        ) : State() {
            var isDisposing = false

            @Suppress("UNCHECKED_CAST")
            fun <A, R : Any> serviceOrNull(key: TypeKey): BoundService<A, R>? =
                registry.getOrPut(key) {
                    component[key]?.bind(graph) ?: return parent?.serviceOrNull(key)
                } as? BoundService<A, R>

            fun <A, R : Any> service(key: TypeKey): BoundService<A, R> = serviceOrNull(key)
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
                plugins.runGraphInitializing(parent, this)
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
            )
        )

        plugins.runGraphInitialized(this)

        instanceOrNullByKey<Unit, Set<TypeKey>>(eagerDependenciesKey, Unit)?.forEach { key ->
            try {
                instanceByKey<Unit, Any>(key, Unit)
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
    ): R = instanceByKey(typeKey<R>(qualifier, generics), Unit)

    /**
     * Retrieve a factory of type `(A) -> R` and apply [argument] to it.
     *
     * @param argument The argument for the factory to retrieve.
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The result of applying [argument] to the retrieved factory.
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified A, reified R : Any> instance(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ): R = instanceByKey(compoundTypeKey<A, R>(qualifier, generics), argument)

    /**
     * Retrieve a factory of type `(A) -> R` by [key] and apply [argument] to it.
     *
     * @param key The type key of the factory.
     * @param argument The argument for the factory to retrieve.
     * @return The result of applying [argument] to the retrieved factory.
     *
     * @throws EntryNotFoundException
     */
    fun <A, R : Any> instanceByKey(
        key: TypeKey,
        argument: A
    ): R = synchronizedMap { it.service<A, R>(key).instance(argument) }

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
    ): R? = instanceOrNullByKey(typeKey<R>(qualifier, generics), Unit)

    /**
     * Retrieve an optional factory of type `(A) -> R` and apply [argument] to it.
     *
     * @param argument The argument for the factory to retrieve.
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The result of applying [argument] to the retrieved factory or null if factory
     *         doesn't exist.
     */
    inline fun <reified A, reified R : Any> instanceOrNull(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ): R? = instanceOrNullByKey(compoundTypeKey<A, R>(qualifier, generics), argument)

    /**
     * Retrieve an optional factory of type `(A) -> R` by [key] and apply [argument] to it.
     *
     * @param key The type key of the factory.
     * @param argument The argument for the factory to retrieve.
     * @return The result of applying [argument] to the retrieved factory or null if factory
     *         doesn't exist.
     */
    fun <A, R : Any> instanceOrNullByKey(key: TypeKey, argument: A): R? =
        synchronizedMap { it.serviceOrNull<A, R>(key)?.instance(argument) }

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
    ): Provider<R> = providerByKey(typeKey<R>(qualifier, generics), Unit)

    /**
     * Retrieves a factory of type `(A) -> R` and creates and returns a
     * [provider][Provider] that applies the given [argument] to the factory when called.
     *
     * @param argument The argument for the factory to retrieve.
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The provider function.
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified A, reified R : Any> provider(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ): Provider<R> = providerByKey(compoundTypeKey<A, R>(qualifier, generics), argument)

    /**
     * Retrieves a factory of type `(A) -> R` by [key] and creates and returns a
     * [provider][Provider] that applies the given [argument] to the factory when called.
     *
     * @param argument The argument for the factory to retrieve.
     * @return The provider function.
     *
     * @throws EntryNotFoundException
     */
    fun <A, R : Any> providerByKey(key: TypeKey, argument: A): Provider<R> = synchronizedMap {
        val service = it.service<A, R>(key)
        return { synchronized(this) { service.instance(argument) } }
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
    ): Provider<R>? = providerOrNullByKey(typeKey<R>(qualifier, generics), Unit)

    /**
     * Retrieves an optional factory of type `(A) -> R` and creates and returns a
     * [provider][Provider] that applies the given [argument] to the factory when called.
     *
     * @param argument The argument for the factory to retrieve.
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The provider function or null if factory doesn't exist.
     */
    inline fun <reified A, reified R : Any> providerOrNull(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ): Provider<R>? = providerOrNullByKey(compoundTypeKey<A, R>(qualifier, generics), argument)

    /**
     * Retrieves an optional factory of type `(A) -> R` by [key] and creates and returns a
     * [provider][Provider] that applies the given [argument] to the factory when called.
     *
     * @param key The type key of the factory.
     * @param argument The argument for the factory to retrieve.
     * @return The provider function or null if factory doesn't exist.
     */
    fun <A, R : Any> providerOrNullByKey(key: TypeKey, argument: A): Provider<R>? =
        synchronizedMap {
            val service = it.serviceOrNull<A, R>(key) ?: return null
            return { synchronized(this) { service.instance(argument) } }
        }

    /**
     * Retrieve a non-optional factory function that takes an argument of type `A` and returns `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The factory that takes `A` and returns `R`
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified A : Any, reified R : Any> factory(
        qualifier: Any? = null,
        generics: Boolean = false
    ): Factory<A, R> = factoryByKey(compoundTypeKey<A, R>(qualifier, generics))

    /**
     * Retrieve a non-optional factory function by [key] that takes an argument of type `A` and
     * returns `R`.
     *
     * @param key The type key of the factory.
     * @return The factory that takes `A` and returns `R`
     *
     * @throws EntryNotFoundException
     */
    fun <A : Any, R : Any> factoryByKey(key: TypeKey): Factory<A, R> = synchronizedMap {
        val service = it.service<A, R>(key)
        return { argument: A -> synchronized(this) { service.instance(argument) } }
    }

    /**
     * Retrieve an optional factory function that takes an argument of type `A` and returns `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The factory that takes `A` and returns `R` or null if factory provider doesn't exist.
     */
    inline fun <reified A : Any, reified R : Any> factoryOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ): Factory<A, R>? = factoryOrNullByKey(compoundTypeKey<A, R>(qualifier, generics))

    /**
     * Retrieve an optional factory function by [key] that takes an argument of type `A` and
     * returns `R`.
     *
     * @param key The type key of the factory.
     * @return The factory that takes `A` and returns `R` or null if factory provider doesn't exist.
     */
    fun <A : Any, R : Any> factoryOrNullByKey(key: TypeKey): Factory<A, R>? = synchronizedMap {
        val service = it.serviceOrNull<A, R>(key) ?: return null
        return { argument: A -> synchronized(this) { service.instance(argument) } }
    }

    /**
     * Retrieve all providers of type `T`.
     *
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return A [Set] of [providers][Provider] of type `T`.
     */
    inline fun <reified T : Any> providersOfType(generics: Boolean = false): Set<Provider<T>> =
        keysOfType(typeKeyOfType<T>(generics))
            .mapTo(mutableSetOf()) { providerByKey<Unit, T>(it, Unit) }

    /**
     * Retrieve all instances of type `T`.
     *
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return A [Set] of instances of type `T`.
     */
    inline fun <reified T : Any> instancesOfType(generics: Boolean = false): Set<T> =
        keysOfType(typeKeyOfType<T>(generics))
            .mapTo(mutableSetOf()) { instanceByKey<Unit, T>(it, Unit) }

    private fun keys(): Set<TypeKey> {
        val keys = component.keys()
        return parent?.keys()?.let { keys + it } ?: keys
    }

    @PublishedApi
    internal fun keysOfType(key: TypeKey): Set<TypeKey> =
        synchronizedMap { state ->
            @Suppress("UNCHECKED_CAST")
            val service = state.registry.getOrPut(key) {
                keys()
                    .filterTo(mutableSetOf()) { it.typeEquals(key) }
                    .run { ConstantService(key, this) }
            } as ConstantService<Set<TypeKey>>
            service.value
        }

    internal fun <A, R : Any> service(key: TypeKey): BoundService<A, R> =
        synchronizedMap { it.service(key) }

    internal fun <A, R : Any> serviceOrNull(key: TypeKey): BoundService<A, R>? =
        synchronizedMap { it.serviceOrNull(key) }

    /**
     * This is called from [BoundService.instance] when a new instance is created.
     * Don't use this method except in custom [BoundService] implementations.
     */
    fun <A, R : Any> evaluate(service: BoundService<A, R>, argument: A): R =
        map { it.serviceEvaluator.evaluate(service, argument) }

    /**
     * Inject members of class [T].
     *
     * @param instance The instance to inject members to.
     * @param injectSuperClasses If true this will look for members injectors for super classes too.
     *
     * @throws WinterException When no members injector was found.
     */
    @JvmOverloads
    fun <T : Any> inject(instance: T, injectSuperClasses: Boolean = false): T {
        synchronizedMap {
            var found = false
            var cls: Class<*>? = instance.javaClass

            while (cls != null) {
                val key = CompoundClassTypeKey(MembersInjector::class.java, cls, null)
                val service = it.serviceOrNull<Unit, MembersInjector<T>>(key)

                if (service != null) {
                    found = true
                    val injector = service.instance(Unit)
                    injector.injectMembers(this, instance)
                }

                if (!injectSuperClasses) break

                cls = cls.superclass
            }

            if (!found) {
                throw WinterException("No members injector found for `${instance.javaClass}`.")
            }

            return instance
        }
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

        val graph = Graph(state.application, this, instance(subcomponentQualifier), name, block)
        state.registry[key] = BoundGraphService(key, graph)
        graph
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

    private fun unregisterSubgraph(sub: Graph) {
        val identifier = sub.identifier ?: return

        synchronizedFold({}, { state ->
            if (state.isDisposing) return
            state.registry.remove(typeKey<Graph>(identifier))
        })
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

                state.plugins.runGraphDispose(this)

                state.registry.values.forEach { boundService -> boundService.dispose() }
                state.parent?.unregisterSubgraph(this)
            } finally {
                this.state = State.Disposed
            }
        }
    }

}
