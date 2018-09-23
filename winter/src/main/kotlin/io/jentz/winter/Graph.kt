@file:Suppress("NOTHING_TO_INLINE")

package io.jentz.winter

import io.jentz.winter.internal.CompoundTypeKey
import io.jentz.winter.internal.MembersInjector

/**
 * The dependency graph class that retrieves and instantiates dependencies from a component.
 *
 * An instance is created by calling [Component.init] or [Graph.initSubcomponent].
 */
class Graph internal constructor(
        /**
         * The parent [Graph] instance.
         */
        val parent: Graph?,

        /**
         * The component instance.
         */
        val component: Component
) {

    private var cache: MutableMap<DependencyKey, BoundService<*, *>>? = mutableMapOf()
    private val stack = mutableListOf<Any?>()
    private var stackSize = 0

    /**
     * Indicates if the graph is disposed.
     */
    var isDisposed = false
        private set

    init {
        @Suppress("UNCHECKED_CAST")
        val entry = component.dependencies[eagerDependenciesKey] as? ConstantService<Set<DependencyKey>>
        entry?.value?.forEach { key ->
            @Suppress("UNCHECKED_CAST")
            val service = serviceOrNull<Unit, Any>(key) as? BoundService<Unit, *>
                    ?: throw EntryNotFoundException("BUG: Eager dependency with key `$key` doesn't exist.")
            service.instance(Unit)
        }
    }


    /**
     * Retrieve a non-optional instance of `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserve generic type parameters.
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
     * @param generics Preserve generic type parameters.
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
     * @return The result of applying [argument] to the retrieved factory or null if factory doesn't exist.
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
     * @param generics Preserve generic type parameters.
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
     * @param generics Preserve generic type parameters.
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
     * @return The provider function or null if factory doesn't exist.
     *
     * @throws EntryNotFoundException
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
     * @param generics Preserve generic type parameters.
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
     * @param generics Preserve generic type parameters.
     * @return The factory that takes `A` and returns `R` or null if factory provider doesn't exist.
     *
     * @throws EntryNotFoundException
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
     * @param generics Preserve generic type parameters.
     * @return A [Set] of [providers][Provider] of type `T`.
     */
    inline fun <reified T : Any> providersOfType(generics: Boolean = false): Set<Provider<T>> {
        return providersOfType(typeKeyOfType<T>(generics))
    }

    @PublishedApi
    internal fun <T : Any> providersOfType(key: DependencyKey): Set<Provider<T>> {
        @Suppress("UNCHECKED_CAST")
        return servicesOfType(key)
                .mapIndexedTo(mutableSetOf()) { _, service ->
                    { service.instance(Unit) }
                } as Set<Provider<T>>
    }

    /**
     * Retrieve all instances of type `T`.
     *
     * @param generics Preserve generic type parameters.
     * @return A [Set] of instances of type `T`.
     */
    inline fun <reified T : Any> instancesOfType(generics: Boolean = false): Set<T> {
        @Suppress("UNCHECKED_CAST")
        return instancesOfType(typeKeyOfType<T>(generics)) as Set<T>
    }

    private fun keys(): Set<DependencyKey> {
        val keys = component.dependencies.keys
        return parent?.keys()?.let { keys + it } ?: keys
    }

    @PublishedApi
    internal fun instancesOfType(key: DependencyKey): Set<*> =
            servicesOfType(key).mapIndexedTo(mutableSetOf()) { _, service -> service.instance(Unit) }

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun servicesOfType(key: DependencyKey): Set<BoundService<Unit, *>> {
        synchronized(this) {
            ensureNotDisposed()

            cache?.get(key)?.let {
                return (it as ConstantService<Set<BoundService<Unit, *>>>).value
            }

            return keys()
                    .filterTo(mutableSetOf()) { it.typeEquals(key) }
                    .mapIndexedTo(mutableSetOf()) { _, key -> service<Unit, Any>(key) }
                    .also { cache?.put(key, ConstantService(key, it)) }
        }
    }


    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <A, R : Any> serviceOrNull(key: DependencyKey): BoundService<A, R>? {
        synchronized(this) {
            ensureNotDisposed()

            cache?.get(key)?.let { return it as BoundService<A, R> }

            val unboundService = component.dependencies[key] ?: return parent?.serviceOrNull(key)

            val boundService = unboundService.bind(this) as BoundService<A, R>
            cache?.set(key, boundService)
            return boundService
        }
    }

    @PublishedApi
    internal inline fun <A, R : Any> service(key: DependencyKey): BoundService<A, R> {
        return serviceOrNull(key)
                ?: throw EntryNotFoundException("Service with key `$key` does not exist.")
    }

    internal inline fun <A, R : Any> evaluate(
            service: BoundService<A, R>,
            argument: A,
            block: () -> R
    ): R {
        ensureNotDisposed()

        val key = service.key

        // check if key is already on the stack
        for (i in 0 until stack.size step 3) {
            if (stack[i + 2] == null && (stack[i] as BoundService<*, *>).key == key) {
                throw CyclicDependencyException("Cyclic dependency for key `$key`.")
            }
        }

        val serviceIndex = stack.size
        val argumentIndex = serviceIndex + 1
        val instanceIndex = argumentIndex + 1

        try {
            // push service
            stack.add(serviceIndex, service)
            // push argument
            stack.add(argumentIndex, argument)
            // set slot for instance to null
            stack.add(instanceIndex, null)
            stackSize += 1
            // create instance and add it to stack
            val instance = block()
            stack[instanceIndex] = instance
            return instance
        } catch (e: EntryNotFoundException) {
            drainStack()
            val stackInfo = stack.joinToString(" -> ")
            throw DependencyResolutionException("Error while resolving dependencies of $key (dependency stack: $stackInfo)", e)
        } catch (e: WinterException) {
            throw e
        } catch (t: Throwable) {
            drainStack()
            val stackInfo = stack.joinToString(" -> ")
            throw DependencyResolutionException("Error while invoking provider block of $key (dependency stack: $stackInfo)", t)
        } finally {
            // decrement stack size
            stackSize -= 1
        }
    }

    internal fun postConstruct() {
        // post init if stack size == 0
        if (stackSize == 0) {
            drainStack()
        }
    }

    private fun drainStack() {
        for (i in stack.size - 1 downTo 0 step 3) {
            val instance = stack[i]
            val argument = stack[i - 1]
            val service = stack[i - 2] as BoundService<*, *>

            if (instance != null && argument != null) {
                service.postConstruct(argument, instance)
            }
        }
        stack.clear()
    }

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
        ensureNotDisposed()

        var found = false
        var cls: Class<*>? = instance.javaClass

        while (cls != null) {
            val key = CompoundTypeKey(MembersInjector::class.java, cls, null)
            @Suppress("UNCHECKED_CAST")
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

    /**
     * Initialize a subcomponent.
     *
     * @param qualifier The qualifier of the subcomponent.
     * @param block An optional builder block to register provider on the subcomponent.
     */
    fun initSubcomponent(qualifier: Any, block: ComponentBuilderBlock? = null): Graph {
        ensureNotDisposed()

        return initializeGraph(this, instance(qualifier), block)
    }

    /**
     * Runs [graph dispose plugins][GraphDisposePlugin] and marks this graph as disposed.
     * All resources get released and every retrieval method will throw an excpetion if called
     * after disposing.
     *
     * Subsequent calls are ignored.
     */
    fun dispose() {
        synchronized(this) {
            if (isDisposed) return

            try {
                WinterPlugins.runGraphDisposePlugins(this)
                cache?.values?.forEach { boundService -> boundService.dispose() }
            } finally {
                isDisposed = true
                cache = null
            }
        }
    }

    private fun ensureNotDisposed() {
        if (isDisposed) throw WinterException("Graph is already disposed.")
    }

}