package io.jentz.winter

import io.jentz.winter.internal.ComponentEntry
import io.jentz.winter.internal.CompoundTypeKey
import io.jentz.winter.internal.ConstantEntry
import io.jentz.winter.internal.MembersInjector
import java.util.*

/**
 * The dependency graph class that retrieves and instantiates dependencies from a component.
 *
 * An instance is created by calling [Component.init] or [Graph.initSubcomponent].
 */
class Graph internal constructor(private val parent: Graph?,
                                 private val dependencies: Map<DependencyKey, ComponentEntry<*>>) {

    private var cache: MutableMap<DependencyKey, Provider<*>>? = mutableMapOf()
    private val stack = Stack<DependencyKey>()

    var isDisposed = false
        private set

    init {
        @Suppress("UNCHECKED_CAST")
        val entry = dependencies[eagerDependenciesKey] as? ConstantEntry<Set<DependencyKey>>
        entry?.value?.forEach { key ->
            val provider = providerOrNull(key) ?: throw EntryNotFoundException("BUG: Eager dependency with key `$key` doesn't exist.")
            provider.invoke()
        }
    }

    /**
     * Retrieve a non-optional instance of `T`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserve generic type parameters.
     * @return An instance of `T`
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified T : Any> instance(qualifier: Any? = null, generics: Boolean = false): T
            = provider<T>(qualifier, generics).invoke()

    /**
     * Retrieve an optional instance of `T`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserve generic type parameters.
     * @return An instance of `T` or null if provider doesn't exist.
     */
    inline fun <reified T : Any> instanceOrNull(qualifier: Any? = null, generics: Boolean = false): T?
            = providerOrNull<T>(qualifier, generics)?.invoke()

    /**
     * Retrieve a non-optional provider function that returns `T`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserve generic type parameters.
     * @return The provider that returns `T`
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified T : Any> provider(qualifier: Any? = null, generics: Boolean = false): () -> T
            = providerOrNull(qualifier, generics) ?: throw EntryNotFoundException("Provider for class `${T::class}` and qualifier `$qualifier` does not exist.")

    /**
     * Retrieve an optional provider function that returns `T`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserve generic type parameters.
     * @return The provider that returns `T` or null if provider doesn't exist.
     */
    inline fun <reified T : Any> providerOrNull(qualifier: Any? = null, generics: Boolean = false): (() -> T)? {
        @Suppress("UNCHECKED_CAST")
        return providerOrNull(typeKey<T>(qualifier, generics)) as? () -> T
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
    inline fun <reified A : Any, reified R : Any> factory(qualifier: Any? = null, generics: Boolean = false): (A) -> R
            = factoryOrNull(qualifier, generics) ?: throw EntryNotFoundException("Factory `(${A::class}) -> ${R::class}` does not exist.")

    /**
     * Retrieve an optional factory function that takes an argument of type `A` and returns `R`.
     *
     * @param qualifier An optional qualifier of the dependency.
     * @param generics Preserve generic type parameters.
     * @return The factory that takes `A` and returns `R` or null if factory provider doesn't exist.
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified A : Any, reified R : Any> factoryOrNull(qualifier: Any? = null, generics: Boolean = false): ((A) -> R)? {
        @Suppress("UNCHECKED_CAST")
        return providerOrNull(compoundTypeKey<A, R>(qualifier, generics))?.invoke() as? (A) -> R
    }

    /**
     * Retrieve all providers of type `T`.
     *
     * @param generics Preserve generic type parameters.
     * @return A [Set] of [providers][Provider] of type `T`.
     */
    inline fun <reified T : Any> providersOfType(generics: Boolean = false): Set<Provider<T>> {
        @Suppress("UNCHECKED_CAST")
        return providersOfType(typeKeyOfType<T>(generics)) as Set<Provider<T>>
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

    /**
     * Retrieve a non-optional provider by [key][DependencyKey].
     */
    @PublishedApi
    internal fun provider(key: DependencyKey): Provider<*> =
            providerOrNull(key) ?: throw EntryNotFoundException("Provider with key `$key` does not exist.")

    /**
     * Retrieve an optional provider by [key][DependencyKey].
     */
    @PublishedApi
    internal fun providerOrNull(key: DependencyKey): Provider<*>? {
        synchronized(this) {
            ensureNotDisposed()

            cache?.get(key)?.let { return it }

            val entry = dependencies[key] ?: return parent?.providerOrNull(key)
            return entry.bind(this).also { cache?.set(key, it) }
        }
    }

    private fun keys(): Set<DependencyKey> {
        val keys = dependencies.keys
        return parent?.keys()?.let { keys + it } ?: keys
    }

    @PublishedApi
    internal fun instancesOfType(key: DependencyKey): Set<*> =
            providersOfType(key).mapIndexedTo(mutableSetOf()) { _, provider -> provider.invoke() }

    @PublishedApi
    internal fun providersOfType(key: DependencyKey): Set<Provider<*>> {
        synchronized(this) {
            ensureNotDisposed()

            cache?.get(key)?.let {
                @Suppress("UNCHECKED_CAST")
                return it() as Set<Provider<*>>
            }

            return keys()
                    .filterTo(mutableSetOf()) { it.typeEquals(key) }
                    .mapIndexedTo(mutableSetOf()) { _, key -> provider(key) }
                    .also { cache?.put(key, { it }) }
        }
    }

    internal fun <T> evaluate(key: DependencyKey, unboundProvider: UnboundProvider<T>): T {
        synchronized(this) {
            ensureNotDisposed()

            if (stack.contains(key)) {
                throw CyclicDependencyException("Cyclic dependency for key `$key`.")
            }
            try {
                stack.push(key)
                return unboundProvider(this)
            } catch (e: EntryNotFoundException) {
                val stackInfo = stack.joinToString(" -> ")
                throw DependencyResolutionException("Error while resolving dependencies of $key (dependency stack: $stackInfo)", e)
            } catch (e: WinterException) {
                throw e
            } catch (t: Throwable) {
                val stackInfo = stack.joinToString(" -> ")
                throw DependencyResolutionException("Error while invoking provider block of $key (dependency stack: $stackInfo)", t)
            } finally {
                stack.pop()
            }
        }
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
            @Suppress("UNCHECKED_CAST")
            val provider = providerOrNull(CompoundTypeKey(MembersInjector::class.java, cls, null)) as? () -> MembersInjector<Any>

            if (provider != null) {
                found = true
                val injector = provider()
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
     *
     * Subsequent calls are ignored.
     */
    fun dispose() {
        synchronized(this) {
            if (isDisposed) return

            try {
                WinterPlugins.runGraphDisposePlugins(this)
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