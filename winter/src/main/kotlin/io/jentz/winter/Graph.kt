package io.jentz.winter

import io.jentz.winter.internal.*
import java.util.*
import kotlin.reflect.KClass

typealias AnyProvider = () -> Any?
typealias AnyFactory = (Any?) -> Any?

/**
 * The dependency graph class that retrieves and instantiates dependencies from a component.
 *
 * An instance is created by calling [Component.init] or [Graph.initSubcomponent].
 */
class Graph internal constructor(private val parent: Graph?, private val component: Component) {

    private val cache = DependencyMap<AnyProvider>(component.dependencyMap.size)
    private val stack = Stack<DependencyId>()

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
            providerOrNull(genericProviderId<T>(qualifier))
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
            providerOrNull(genericFactoryId<A, R>(qualifier))
        } else {
            providerOrNull(A::class, R::class, qualifier)
        }?.invoke() as? (A) -> R
    }

    /**
     * Retrieve a non-optional provider by [ID][DependencyId].
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param id A dependency ID
     * @return The [provider][AnyProvider]
     *
     * @throws EntryNotFoundException
     * @suppress
     */
    fun provider(id: DependencyId) = providerOrNull(id) ?: throw EntryNotFoundException("Provider with ID `$id` does not exist.")

    /**
     * Retrieve an optional provider by [ID][DependencyId].
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param id A dependency ID
     * @return The [provider][AnyProvider] or null if provider doesn't exist.
     *
     * @suppress
     */
    fun providerOrNull(id: DependencyId): AnyProvider? = retrieve(
            getCached = { cache[id] },
            getEntry = { component.dependencyMap.getEntry(id) },
            getParent = { parent?.providerOrNull(id) })

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
                                getEntry: () -> DependencyMap.Entry<ComponentEntry>?,
                                getParent: () -> AnyProvider?): AnyProvider? = synchronized(this) {
        getCached()?.let { return@synchronized it }

        val entry = getEntry()

        if (entry != null) {
            val value = entry.value

            val provider: AnyProvider = when (value) {
                is ConstantEntry<*> -> wrapConstant(value)
                is ProviderEntry<*> -> wrapProvider(entry.key, value.bind(this))
                is FactoryEntry<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    wrapFactory(entry.key, value.bind(this) as AnyFactory)
                }
            }

            cache[entry.key] = provider

            return@synchronized provider
        }

        return@synchronized getParent()
    }


    private fun wrapConstant(constant: ConstantEntry<*>): AnyProvider = { constant.value }

    private fun wrapProvider(id: DependencyId, block: AnyProvider): AnyProvider = {
        synchronized(this) {
            if (stack.contains(id)) {
                throw CyclicDependencyException("Cyclic dependency for ID `$id`.")
            }
            try {
                stack.push(id)
                block()
            } catch (e: EntryNotFoundException) {
                val stackInfo = stack.joinToString(" -> ")
                throw DependencyResolutionException("Error while resolving dependencies of $id (dependency stack: $stackInfo)", e)
            } catch (e: WinterException) {
                throw e
            } catch (t: Throwable) {
                val stackInfo = stack.joinToString(" -> ")
                throw DependencyResolutionException("Error while invoking provider block of $id (dependency stack: $stackInfo)", t)
            } finally {
                stack.pop()
            }
        }
    }

    private fun wrapFactory(id: DependencyId, block: AnyFactory): AnyProvider {
        val wrapped = { arg: Any? ->
            synchronized(this) {
                if (stack.contains(id)) {
                    throw CyclicDependencyException("Cyclic dependency for ID `$id`.")
                }
                try {
                    stack.push(id)
                    block(arg)
                } catch (e: EntryNotFoundException) {
                    val stackInfo = stack.joinToString(" -> ")
                    throw DependencyResolutionException("Error while resolving dependencies of $id (dependency stack: $stackInfo)", e)
                } catch (e: WinterException) {
                    throw e
                } catch (t: Throwable) {
                    val stackInfo = stack.joinToString(" -> ")
                    throw DependencyResolutionException("Error while invoking factory block of $id (dependency stack: $stackInfo)", t)
                } finally {
                    stack.pop()
                }
            }
        }
        return { wrapped }
    }

    /**
     * Initialize a subcomponent.
     *
     * @param name The name of the subcomponent.
     * @param block An optional builder block to register provider on the subcomponent.
     */
    fun initSubcomponent(name: String, block: (ComponentBuilder.() -> Unit)? = null): Graph {
        val subComponent: Component = instance(name)
        return Graph(this, if (block != null) subComponent.derive(block) else subComponent)
    }

}