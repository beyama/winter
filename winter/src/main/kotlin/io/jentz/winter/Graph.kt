package io.jentz.winter

import io.jentz.winter.internal.*
import java.util.*

/**
 * The dependency graph class that retrieves and instantiates dependencies from a component.
 *
 * An instance is created by calling [Component.init] or [Graph.initSubcomponent].
 */
class Graph internal constructor(private val parent: Graph?,
                                 private val dependencyMap: DependencyMap<ComponentEntry<*>>) {

    private val cache = DependencyMap<Provider<*>>(dependencyMap.size)
    private val stack = Stack<DependencyKey>()

    init {
        @Suppress("UNCHECKED_CAST")
        val entry = dependencyMap[eagerDependenciesKey] as? ConstantEntry<Set<DependencyKey>>
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
        return if (generics) {
            providerOrNull(genericTypeKey<T>(qualifier))
        } else {
            providerOrNull(T::class.javaObjectType, qualifier)
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
            providerOrNull(A::class.javaObjectType, R::class.javaObjectType, qualifier)
        }?.invoke() as? (A) -> R
    }

    /**
     * Retrieve a non-optional provider by [key][DependencyKey].
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param key A dependency key
     * @return The [provider][Provider]
     *
     * @throws EntryNotFoundException
     * @suppress
     */
    fun provider(key: DependencyKey): Provider<*> =
            providerOrNull(key) ?: throw EntryNotFoundException("Provider with key `$key` does not exist.")

    /**
     * Retrieve an optional provider by [key][DependencyKey].
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param key A dependency key
     * @return The [provider][Provider] or null if provider doesn't exist.
     *
     * @suppress
     */
    fun providerOrNull(key: DependencyKey): Provider<*>? = retrieve(
            getCached = { cache[key] },
            getEntry = { dependencyMap.getEntry(key) },
            getParent = { parent?.providerOrNull(key) })

    /**
     * Retrieve an optional provider by class and qualifier.
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param cls The return class of the requested provider.
     * @param qualifier An optional qualifier of the requested provider.
     * @return The [provider][Provider] or null if provider doesn't exist.
     *
     * @suppress
     */
    fun providerOrNull(cls: Class<*>, qualifier: Any? = null): Provider<*>? = retrieve(
            getCached = { cache.get(cls, qualifier) },
            getEntry = { dependencyMap.getEntry(cls, qualifier) },
            getParent = { parent?.providerOrNull(cls, qualifier) })

    /**
     * Retrieve an optional factory provider by argument and return class and qualifier.
     *
     * `THIS ISN'T PART OF THE PUBLIC API`
     *
     * @param firstClass The argument class of the requested factory provider.
     * @param secondClass The return class of the requested factory provider.
     * @param qualifier An optional qualifier of the requested factory provider.
     * @return The [provider][Provider] or null if provider doesn't exist.
     *
     * @suppress
     */
    fun providerOrNull(firstClass: Class<*>, secondClass: Class<*>, qualifier: Any? = null): Provider<*>? = retrieve(
            getCached = { cache.get(firstClass, secondClass, qualifier) },
            getEntry = { dependencyMap.getEntry(firstClass, secondClass, qualifier) },
            getParent = { parent?.providerOrNull(firstClass, secondClass, qualifier) })

    private inline fun retrieve(getCached: () -> Provider<*>?,
                                getEntry: () -> DependencyMap.Entry<ComponentEntry<*>>?,
                                getParent: () -> Provider<*>?): Provider<*>? = synchronized(this) {
        getCached()?.let { return@synchronized it }

        return@synchronized getEntry()?.let { entry ->
            entry.value
                    .bind(this)
                    .also { cache[entry.key] = it }
        } ?: getParent()
    }

    internal fun <T> evaluate(key: DependencyKey, unboundProvider: UnboundProvider<T>): T {
        synchronized(this) {
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
        var found = false
        var cls: Class<*>? = instance.javaClass

        while (cls != null) {
            @Suppress("UNCHECKED_CAST")
            val provider = providerOrNull(MembersInjector::class.java, cls) as? () -> MembersInjector<Any>

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
        val subComponent: Component = instance(qualifier)
        return initializeGraph(this, subComponent, block)
    }

}