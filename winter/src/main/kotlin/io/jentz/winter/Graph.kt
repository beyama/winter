package io.jentz.winter

import io.jentz.winter.internal.*
import java.util.*
import kotlin.reflect.KClass

typealias AnyProvider = () -> Any?

/**
 * The dependency graph class that retrieves and instantiates dependencies from a component.
 *
 * An instance is created by calling [Component.init] or [Graph.initSubcomponent].
 */
class Graph internal constructor(private val parent: Graph?, private val component: Component) {

    private val cache = DependencyMap<AnyProvider>(component.dependencyMap.size)
    private val stack = Stack<DependencyKey>()

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
        return if (generics) {
            providerOrNull(genericTypeKey<T>(qualifier))
        } else {
            providerOrNull(T::class, qualifier)
        } as? () -> T
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
        return if (generics) {
            providerOrNull(genericCompoundTypeKey<A, R>(qualifier))
        } else {
            providerOrNull(A::class, R::class, qualifier)
        }?.invoke() as? (A) -> R
    }

    /**
     * Retrieve a non-optional provider by [key][DependencyKey].
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param key A dependency key
     * @return The [provider][AnyProvider]
     *
     * @throws EntryNotFoundException
     * @suppress
     */
    fun provider(key: DependencyKey) = providerOrNull(key) ?: throw EntryNotFoundException("Provider with key `$key` does not exist.")

    /**
     * Retrieve an optional provider by [key][DependencyKey].
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param key A dependency key
     * @return The [provider][AnyProvider] or null if provider doesn't exist.
     *
     * @suppress
     */
    fun providerOrNull(key: DependencyKey): AnyProvider? = retrieve(
            getCached = { cache[key] },
            getEntry = { component.dependencyMap.getEntry(key) },
            getParent = { parent?.providerOrNull(key) })

    /**
     * Retrieve an optional provider by class and qualifier.
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param kClass The return class of the requested provider.
     * @param qualifier An optional qualifier of the requested provider.
     * @return The [provider][AnyProvider] or null if provider doesn't exist.
     *
     * @suppress
     */
    fun providerOrNull(kClass: KClass<*>, qualifier: Any? = null): AnyProvider? = retrieve(
            getCached = { cache.get(kClass, qualifier) },
            getEntry = { component.dependencyMap.getEntry(kClass, qualifier) },
            getParent = { parent?.providerOrNull(kClass, qualifier) })

    /**
     * Retrieve an optional factory provider by argument and return class and qualifier.
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param argClass The argument class of the requested factory provider.
     * @param retClass The return class of the requested factory provider.
     * @param qualifier An optional qualifier of the requested factory provider.
     * @return The [provider][AnyProvider] or null if provider doesn't exist.
     *
     * @suppress
     */
    fun providerOrNull(argClass: KClass<*>, retClass: KClass<*>, qualifier: Any? = null): AnyProvider? = retrieve(
            getCached = { cache.get(argClass, retClass, qualifier) },
            getEntry = { component.dependencyMap.getEntry(argClass, retClass, qualifier) },
            getParent = { parent?.providerOrNull(argClass, retClass, qualifier) })

    private inline fun retrieve(getCached: () -> AnyProvider?,
                                getEntry: () -> DependencyMap.Entry<ComponentEntry<*>>?,
                                getParent: () -> AnyProvider?): AnyProvider? = synchronized(this) {
        getCached()?.let { return@synchronized it }

        return@synchronized getEntry()?.let { entry ->
            entry.value.bind(this, entry.key).also { cache[entry.key] = it }
        } ?: getParent()
    }

    fun <A, R> evaluateFactory(key: DependencyKey, arg: A, block: Graph.(A) -> R): R {
        return evaluate(key, { block(arg) })
    }

    fun <T> evaluateProvider(key: DependencyKey, block: Graph.() -> T): T {
        return evaluate(key, block)
    }

    private inline fun <T> evaluate(key: DependencyKey, block: Graph.() -> T): T {
        synchronized(this) {
            if (stack.contains(key)) {
                throw CyclicDependencyException("Cyclic dependency for key `$key`.")
            }
            try {
                stack.push(key)
                return block()
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
     * Initialize a subcomponent.
     *
     * @param qualifier The qualifier of the subcomponent.
     * @param block An optional builder block to register provider on the subcomponent.
     */
    fun initSubcomponent(qualifier: Any, block: (ComponentBuilder.() -> Unit)? = null): Graph {
        val subComponent: Component = instance(qualifier)
        return Graph(this, if (block != null) subComponent.derive(block) else subComponent)
    }

}