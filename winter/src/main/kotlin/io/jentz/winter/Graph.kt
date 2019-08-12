package io.jentz.winter

/**
 * The object graph class that retrieves and instantiates dependencies registered in its component.
 *
 * An instance is created by calling [Component.createGraph], [Graph.createSubgraph]
 * or [Graph.openSubgraph].
 */
class Graph internal constructor(
    val application: WinterApplication,
    parent: Graph?,
    component: Component,
    // only set for graphs that are managed (opened) by the parent graph
    private val identifier: Any?,
    block: ComponentBuilderBlock?
) {

    private sealed class State {
        data class Initialized(
            val component: Component,
            val parent: Graph?,
            val stack: DependenciesStack,
            val registry: MutableMap<TypeKey, BoundService<*, *>> = mutableMapOf()
        ) : State() {
            var isDisposing = false
        }

        object Disposed : State()
    }

    private var state: State

    private inline fun <T> withState(block: (State) -> T): T = synchronized(this) { block(state) }

    private inline fun <T> fold(ifDisposed: () -> T, ifInitialized: (State.Initialized) -> T): T =
        withState { state ->
            when (state) {
                is State.Disposed -> ifDisposed()
                is State.Initialized -> ifInitialized(state)
            }
        }

    private inline fun <T> map(block: (State.Initialized) -> T): T =
        fold({ throw WinterException("Graph is already disposed.") }, block)

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
        val baseComponent = if (application.plugins.isNotEmpty() || block != null) {
            component.derive {
                block?.invoke(this)
                application.plugins.runInitializingComponent(parent, this)
            }
        } else {
            component
        }

        state = State.Initialized(baseComponent, parent, DependenciesStack(this))

        val eagerDependencies = serviceOrNull<Unit, Set<TypeKey>>(eagerDependenciesKey)
        eagerDependencies?.instance(Unit)?.forEach { key ->
            val service = serviceOrNull<Unit, Any>(key)
                ?: throw EntryNotFoundException(
                    key,
                    "BUG: Eager dependency with key `$key` doesn't exist."
                )
            service.instance(Unit)
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
    ): R {
        val key = typeKey<R>(qualifier, generics)
        return service<Unit, R>(key).instance(Unit)
    }

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
    ): R {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        return service<A, R>(key).instance(argument)
    }

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
    ): R? {
        val key = typeKey<R>(qualifier, generics)
        return serviceOrNull<Unit, R>(key)?.instance(Unit)
    }

    /**
     * Retrieve an optional factory of type `(A) -> R` and apply [argument] to it.
     *
     * @param argument The argument for the factory to retrieve.
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return The result of applying [argument] to the retrieved factory or null if factory
     *         doesn't exist.
     *
     */
    inline fun <reified A, reified R : Any> instanceOrNull(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ): R? {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        return serviceOrNull<A, R>(key)?.instance(argument)
    }

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
    ): Provider<R> {
        val key = typeKey<R>(qualifier, generics)
        val service = service<Unit, R>(key)
        return { service.instance(Unit) }
    }

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
    ): Provider<R> {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        val service = service<A, R>(key)
        return { service.instance(argument) }
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
    ): Provider<R>? {
        val key = typeKey<R>(qualifier, generics)
        val service = serviceOrNull<Unit, R>(key) ?: return null
        return { service.instance(Unit) }
    }

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
    ): Provider<R>? {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        val service = serviceOrNull<A, R>(key) ?: return null
        return { service.instance(argument) }
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
    ): Factory<A, R> {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        val service = service<A, R>(key)
        return { argument: A -> service.instance(argument) }
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
    ): Factory<A, R>? {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        val service = serviceOrNull<A, R>(key) ?: return null
        return { argument: A -> service.instance(argument) }
    }

    /**
     * Retrieve all providers of type `T`.
     *
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return A [Set] of [providers][Provider] of type `T`.
     */
    inline fun <reified T : Any> providersOfType(generics: Boolean = false): Set<Provider<T>> {
        return providersOfType(typeKeyOfType<T>(generics))
    }

    @PublishedApi
    internal fun <T : Any> providersOfType(key: TypeKey): Set<Provider<T>> {
        @Suppress("UNCHECKED_CAST")
        return servicesOfType(key)
            .mapIndexedTo(mutableSetOf()) { _, service ->
                { service.instance(Unit) }
            } as Set<Provider<T>>
    }

    /**
     * Retrieve all instances of type `T`.
     *
     * @param generics Preserves generic type parameters if set to true (default = false).
     * @return A [Set] of instances of type `T`.
     */
    inline fun <reified T : Any> instancesOfType(generics: Boolean = false): Set<T> {
        @Suppress("UNCHECKED_CAST")
        return instancesOfType(typeKeyOfType<T>(generics)) as Set<T>
    }

    private fun keys(): Set<TypeKey> {
        val keys = component.keys()
        return parent?.keys()?.let { keys + it } ?: keys
    }

    @PublishedApi
    internal fun instancesOfType(key: TypeKey): Set<*> =
        servicesOfType(key).mapIndexedTo(mutableSetOf()) { _, service -> service.instance(Unit) }

    @PublishedApi
    internal fun servicesOfType(key: TypeKey): Set<BoundService<Unit, *>> =
        map { (_, _, _, registry) ->
            @Suppress("UNCHECKED_CAST")
            val service = registry.getOrPut(key) {
                keys()
                    .asSequence()
                    .filterTo(mutableSetOf()) { it.typeEquals(key) }
                    .mapTo(mutableSetOf()) { key -> service<Unit, Any>(key) }
                    .run { ConstantService(key, this) }
            } as ConstantService<Set<BoundService<Unit, *>>>
            service.value
        }

    @PublishedApi
    internal fun <A, R : Any> serviceOrNull(key: TypeKey): BoundService<A, R>? =
        @Suppress("UNCHECKED_CAST")
        map { (component, parent, _, registry) ->
            registry.getOrPut(key) {
                component[key]?.bind(this) ?: return parent?.serviceOrNull(key)
            } as? BoundService<A, R>
        }

    @PublishedApi
    internal fun <A, R : Any> service(key: TypeKey): BoundService<A, R> {
        return serviceOrNull(key)
            ?: throw EntryNotFoundException(key, "Service with key `$key` does not exist.")
    }

    /**
     * This is called from [BoundService.instance] when a new instance is created.
     * Don't use this method except in custom [BoundService] implementations.
     */
    fun <A, R : Any> evaluate(service: BoundService<A, R>, argument: A): R =
        map { (_, _, stack, _) -> stack.evaluate(service, argument) }

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
        map {
            var found = false
            var cls: Class<*>? = instance.javaClass

            while (cls != null) {
                val key = CompoundClassTypeKey(MembersInjector::class.java, cls, null)
                val service = serviceOrNull<Unit, MembersInjector<T>>(key)

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
     * @see createSubgraph
     */
    @Deprecated(
        "Use createSubgraph instead.",
        ReplaceWith("createSubgraph(qualifier,block)")
    )
    fun initSubcomponent(qualifier: Any, block: ComponentBuilderBlock? = null): Graph =
        createSubgraph(qualifier, block)

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
    ): Graph = map {
        Graph(application, this, instance(subcomponentQualifier), null, block)
    }

    /**
     * @see createSubgraph
     */
    @Deprecated(
        "Use createSubgraph instead.",
        ReplaceWith("createSubgraph(subcomponentQualifier,block)")
    )
    fun createChildGraph(
        subcomponentQualifier: Any,
        block: ComponentBuilderBlock? = null
    ): Graph = createSubgraph(subcomponentQualifier, block)

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
    ): Graph = map { (_, _, _, registry) ->
        val name = identifier ?: subcomponentQualifier
        val key = typeKey<Graph>(name)

        if (key in registry) {
            throw WinterException(
                "Cannot open subgraph with identifier `$name` because it is already open."
            )
        }

        val graph = Graph(application, this, instance(subcomponentQualifier), name, block)
        registry[key] = BoundGraphService(key, graph)
        graph
    }

    /**
     * @see openSubgraph
     */
    @Deprecated(
        "Use openSubgraph instead.",
        ReplaceWith("openSubgraph(subcomponentQualifier,identifier,block)")
    )
    fun openChildGraph(
        subcomponentQualifier: Any,
        identifier: Any? = null,
        block: ComponentBuilderBlock? = null
    ): Graph = openSubgraph(subcomponentQualifier, identifier, block)

    /**
     * Close a subgraph by disposing it and removing it from the registry.
     *
     * @param identifier The identifier it was opened with.
     */
    fun closeSubgraph(identifier: Any) {
        map { (_, _, _, registry) ->
            val key = typeKey<Graph>(identifier)
            val service = registry.remove(key) ?: throw WinterException(
                "Subgraph with identifier `$identifier` doesn't exist."
            )
            service.dispose()
        }
    }

    @Deprecated(
        "Use closeSubgraph instead.",
        ReplaceWith("closeSubgraph(identifier)")
    )
    fun closeChildGraph(identifier: Any) = closeSubgraph(identifier)

    private fun unregisterSubgraph(sub: Graph) {
        val identifier = sub.identifier ?: return

        fold({}, { state ->
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
        fold({}) { state ->
            try {
                if (state.isDisposing) return

                state.isDisposing = true

                application.plugins.runGraphDispose(this)

                state.registry.values.forEach { boundService -> boundService.dispose() }
                state.parent?.unregisterSubgraph(this)
            } finally {
                this.state = State.Disposed
            }
        }
    }

}
