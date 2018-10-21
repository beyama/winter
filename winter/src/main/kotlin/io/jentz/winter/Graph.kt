package io.jentz.winter

/**
 * The dependency graph class that retrieves and instantiates dependencies from a component.
 *
 * An instance is created by calling [Component.init] or [Graph.initSubcomponent].
 */
class Graph internal constructor(parent: Graph?, component: Component) {

    private sealed class State {
        data class Initialized(
            val component: Component,
            val parent: Graph?,
            val stack: DependenciesStack,
            val cache: MutableMap<TypeKey, BoundService<*, *>> = mutableMapOf()
        ) : State()

        object Disposed : State()
    }

    private var state: State = State.Initialized(component, parent, DependenciesStack(this))

    private inline fun <T> fold(ifDisposed: () -> T, ifInitialized: (State.Initialized) -> T): T {
        return synchronized(this) {
            val state = state
            if (state is State.Initialized) ifInitialized(state) else ifDisposed()
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
        val keys = component.dependencies.keys
        return parent?.keys()?.let { keys + it } ?: keys
    }

    @PublishedApi
    internal fun instancesOfType(key: TypeKey): Set<*> =
        servicesOfType(key).mapIndexedTo(mutableSetOf()) { _, service -> service.instance(Unit) }

    @PublishedApi
    internal fun servicesOfType(key: TypeKey): Set<BoundService<Unit, *>> =
        map { (_, _, _, cache) ->
            @Suppress("UNCHECKED_CAST")
            val service = cache.getOrPut(key) {
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
        map { (component, parent, _, cache) ->
            cache.getOrPut(key) {
                component.dependencies[key]?.bind(this) ?: return parent?.serviceOrNull(key)
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
     * Initialize a subcomponent.
     *
     * @param qualifier The qualifier of the subcomponent.
     * @param block An optional builder block to register provider on the subcomponent.
     */
    fun initSubcomponent(qualifier: Any, block: ComponentBuilderBlock? = null): Graph =
        map { initializeGraph(this, instance(qualifier), block) }

    /**
     * Runs [graph dispose plugins][GraphDisposePlugin] and marks this graph as disposed.
     * All resources get released and every retrieval method will throw an excpetion if called
     * after disposing.
     *
     * Subsequent calls are ignored.
     */
    fun dispose() {
        fold({}) { (_, _, _, cache) ->
            try {
                WinterPlugins.runGraphDisposePlugins(this)
                cache.values.forEach { boundService -> boundService.dispose() }
            } finally {
                state = State.Disposed
            }
        }
    }

}
