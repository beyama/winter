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
            val registry: MutableMap<TypeKey<*, *>, BoundService<*, *>> = mutableMapOf(),
            val onDisposeCallback: OnDisposeCallback?
        ) : State() {
            var isDisposing = false

            @Suppress("UNCHECKED_CAST")
            fun <A, R : Any> serviceOrNull(key: TypeKey<A, R>): BoundService<A, R>? =
                registry.getOrPut(key) {
                    component[key]?.bind(graph) ?: return parent?.serviceOrNull(key)
                } as? BoundService<A, R>

            fun <A, R : Any> service(key: TypeKey<A, R>): BoundService<A, R> = serviceOrNull(key)
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

        instanceOrNullByKey(eagerDependenciesKey, Unit)?.forEach { key ->
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
    ): R = instanceByKey(compoundTypeKey(qualifier, generics), argument)

    /**
     * Retrieve a non-optional instance of `R` by [key].
     *
     * @param key The type key of the instance.
     * @return An instance of `R`
     *
     * @throws EntryNotFoundException
     */
    fun <R : Any> instanceByKey(key: TypeKey<Unit, R>): R = instanceByKey(key, Unit)

    /**
     * Retrieve a factory of type `(A) -> R` by [key] and apply [argument] to it.
     *
     * @param key The type key of the factory.
     * @param argument The argument for the factory to retrieve.
     * @return The result of applying [argument] to the retrieved factory.
     *
     * @throws EntryNotFoundException
     */
    fun <A, R : Any> instanceByKey(key: TypeKey<A, R>, argument: A): R =
        synchronizedMap { it.service(key).instance(argument) }

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
    ): R? = instanceOrNullByKey(compoundTypeKey(qualifier, generics), argument)

    /**
     * Retrieve an optional instance of `R` by [key].
     *
     * @param key The type key of the instance.
     * @return An instance of `R` or null if provider doesn't exist.
     */
    fun <R : Any> instanceOrNullByKey(key: TypeKey<Unit, R>): R? = instanceOrNullByKey(key, Unit)

    /**
     * Retrieve an optional factory of type `(A) -> R` by [key] and apply [argument] to it.
     *
     * @param key The type key of the factory.
     * @param argument The argument for the factory to retrieve.
     * @return The result of applying [argument] to the retrieved factory or null if factory
     *         doesn't exist.
     */
    fun <A, R : Any> instanceOrNullByKey(key: TypeKey<A, R>, argument: A): R? =
        synchronizedMap { it.serviceOrNull(key)?.instance(argument) }

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
    ): Provider<R> = providerByKey(compoundTypeKey(qualifier, generics), argument)

    /**
     * Retrieves a non-optional provider function by [key] that returns `R`.
     *
     * @param key The type key of the instance.
     * @return The provider function.
     *
     * @throws EntryNotFoundException
     */
    fun <R : Any> providerByKey(key: TypeKey<Unit, R>): Provider<R> = providerByKey(key, Unit)

    /**
     * Retrieves a factory of type `(A) -> R` by [key] and creates and returns a
     * [provider][Provider] that applies the given [argument] to the factory when called.
     *
     * @param key The type key of the factory.
     * @param argument The argument for the factory to retrieve.
     * @return The provider function.
     *
     * @throws EntryNotFoundException
     */
    fun <A, R : Any> providerByKey(key: TypeKey<A, R>, argument: A): Provider<R> = synchronizedMap {
        val service = it.service(key)
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
    ): Provider<R>? = providerOrNullByKey(typeKey(qualifier, generics))

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
    ): Provider<R>? = providerOrNullByKey(compoundTypeKey(qualifier, generics), argument)

    /**
     * Retrieve an optional provider function by [key] that returns `R`.
     *
     * @param key The type key of the instance.
     * @return The provider that returns `R` or null if provider doesn't exist.
     */
    fun <R : Any> providerOrNullByKey(key: TypeKey<Unit, R>): Provider<R>? =
        providerOrNullByKey(key, Unit)

    /**
     * Retrieves an optional factory of type `(A) -> R` by [key] and creates and returns a
     * [provider][Provider] that applies the given [argument] to the factory when called.
     *
     * @param key The type key of the factory.
     * @param argument The argument for the factory to retrieve.
     * @return The provider function or null if factory doesn't exist.
     */
    fun <A, R : Any> providerOrNullByKey(key: TypeKey<A, R>, argument: A): Provider<R>? =
        synchronizedMap {
            val service = it.serviceOrNull(key) ?: return null
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
    inline fun <reified A, reified R : Any> factory(
        qualifier: Any? = null,
        generics: Boolean = false
    ): Factory<A, R> = factoryByKey(compoundTypeKey(qualifier, generics))

    /**
     * Retrieve a non-optional factory function by [key] that takes an argument of type `A` and
     * returns `R`.
     *
     * @param key The type key of the factory.
     * @return The factory that takes `A` and returns `R`
     *
     * @throws EntryNotFoundException
     */
    fun <A, R : Any> factoryByKey(key: TypeKey<A, R>): Factory<A, R> = synchronizedMap {
        val service = it.service(key)
        return { argument: A -> synchronized(this) { service.instance(argument) } }
    }

    /**
     * Retrieve an optional factory function that takes an argument of type `A` and returns `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The factory that takes `A` and returns `R` or null if factory provider doesn't exist.
     */
    inline fun <reified A, reified R : Any> factoryOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ): Factory<A, R>? = factoryOrNullByKey(compoundTypeKey(qualifier, generics))

    /**
     * Retrieve an optional factory function by [key] that takes an argument of type `A` and
     * returns `R`.
     *
     * @param key The type key of the factory.
     * @return The factory that takes `A` and returns `R` or null if factory provider doesn't exist.
     */
    fun <A, R : Any> factoryOrNullByKey(key: TypeKey<A, R>): Factory<A, R>? = synchronizedMap {
        val service = it.serviceOrNull(key) ?: return null
        return { argument: A -> synchronized(this) { service.instance(argument) } }
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
    fun <R : Any> instancesOfTypeByKey(key: TypeKey<Unit, R>): Set<R> {
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
    fun <R : Any> providersOfTypeByKey(
        key: TypeKey<Unit, R>
    ): Set<Provider<R>> {
        require(key.qualifier == TYPE_KEY_OF_TYPE_QUALIFIER) {
            "Type key qualifier must be `$TYPE_KEY_OF_TYPE_QUALIFIER`."
        }
        return keysOfType(key).mapTo(mutableSetOf()) { providerByKey(it) }
    }

    private fun <R : Any> keysOfType(
        key: TypeKey<Unit, R>
    ): Set<TypeKey<Unit, R>> = synchronizedMap { state ->
        @Suppress("UNCHECKED_CAST")
        val service = state.registry.getOrPut(key) {
            val keys = keys().filter { it.typeEquals(key) }.toSet()
            ConstantService(key, keys)
        } as ConstantService<Set<TypeKey<Unit, R>>>
        service.value
    }

    private fun keys(): Set<TypeKey<*, *>> {
        val keys = component.keys()
        return parent?.keys()?.let { keys + it } ?: keys
    }

    internal fun <A, R : Any> service(key: TypeKey<A, R>): BoundService<A, R> =
        synchronizedMap { it.service(key) }

    internal fun <A, R : Any> serviceOrNull(key: TypeKey<A, R>): BoundService<A, R>? =
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
                val key = membersInjectorKey(cls)
                val service = it.serviceOrNull(key)

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
