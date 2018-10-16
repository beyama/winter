package io.jentz.winter

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * The Injector provides a was to inject dependencies into properties via Kotlins property
 * delegates.
 *
 * E.g.
 * ```
 * class MyClass {
 *   private val injector = Injector()
 *   private val eagerService: Service1 by injector.instance()
 *   private val lazyService: Service2 by injector.lazyInstance()
 *   private val fromFactory: Widget by injector.instance(Color.BLUE)
 *
 *   fun onCreate() {
 *     injector.inject(Application.graph)
 *   }
 *
 * }
 * ```
 *
 */
class Injector {
    private var propertyInjectors: MutableList<InjectedProperty<*>>? = mutableListOf()

    val injected get() = propertyInjectors == null

    /**
     * Creates and registers a property delegate for a [Provider] of type `() -> T`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> provider(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(ProviderProperty<Unit, T>(typeKey<T>(qualifier, generics), Unit))

    /**
     * Creates and registers a property delegate for a [Provider] of type `() -> R`.
     * This lookups a factory of type `(A) -> R` and calls it with the supplied argument when
     * the provider gets invoked.
     *
     * @param argument The argument for the factory.
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R : Any> provider(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(ProviderProperty<A, R>(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a property delegate for an optional [Provider] of type `T`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> providerOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(ProviderOrNullProperty<Unit, T>(typeKey<T>(qualifier, generics), Unit))

    /**
     * Creates and registers a property delegate for a [Provider] of type `() -> R`.
     * This lookups a factory of type `(A) -> R` and calls it with the supplied argument when
     * the provider gets invoked. The delegate will return null when no factory of type `(A) -> R`
     * exists.
     *
     * @param argument The argument for the factory.
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R : Any> providerOrNull(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(ProviderOrNullProperty<A, R>(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a property delegate for an instance of type `T`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> instance(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(InstanceProperty<Unit, T>(typeKey<T>(qualifier, generics), Unit))

    /**
     * Creates and registers a property delegate for an instance of type `R`.
     * This lookups a factory of type `(A) -> R` and calls it with the supplied argument.
     *
     * @param argument The argument for the factory.
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R : Any> instance(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(InstanceProperty<A, R>(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a property delegate for an optional instance of type `T`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> instanceOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(InstanceOrNullProperty<Unit, T>(typeKey<T>(qualifier, generics), Unit))

    /**
     * Creates and registers a property delegate for an optional instance of type `R`.
     * This lookups a factory of type `(A) -> R` and calls it with the supplied argument or
     * returns null if no factory of type `(A) -> R` exists.
     *
     * @param argument The argument for the factory.
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R : Any> instanceOrNull(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(InstanceOrNullProperty<A, R>(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a lazy property delegate for an instance of type `T`.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> lazyInstance(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(LazyInstanceProperty<Unit, T>(typeKey<T>(qualifier, generics), Unit))

    /**
     * Creates and registers a lazy property delegate for an instance of type `R`.
     *
     * This lookups a factory of type `(A) -> R` and calls it with the supplied argument.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param argument The argument for the factory.
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R : Any> lazyInstance(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(LazyInstanceProperty<A, R>(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a lazy property delegate for an optional instance of type `T`.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> lazyInstanceOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(LazyInstanceOrNullProperty<Unit, T>(typeKey<T>(qualifier, generics), Unit))

    /**
     * Creates and registers a lazy property delegate for an optional instance of type `R`.
     *
     * This lookups a factory of type `(A) -> R` and calls it with the supplied argument or
     * returns null if no factory of type `(A) -> R` exists.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param argument The argument for the factory.
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R : Any> lazyInstanceOrNull(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(
        LazyInstanceOrNullProperty<A, R>(
            compoundTypeKey<A, R>(qualifier, generics),
            argument
        )
    )

    /**
     * Creates and registers a property delegate for a [Set] of [providers][Provider] of type `T`.
     *
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> providersOfType(generics: Boolean = false) =
        register(ProvidersOfTypeProperty<T>(typeKeyOfType<T>(generics)))

    /**
     * Creates and registers a property delegate for a [Set] of instances of type `T`.
     *
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> instancesOfType(generics: Boolean = false) =
        register(InstancesOfTypeProperty<T>(typeKeyOfType<T>(generics)))

    /**
     * Creates and registers a lazy property delegate for a [Set] of instances of type `T`.
     *
     * The instances get retrieved/created on first property access.
     *
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> lazyInstancesOfType(generics: Boolean = false) =
        register(LazyInstancesOfTypeProperty<T>(typeKeyOfType<T>(generics)))

    /**
     * Creates and registers a property delegate for a factory of type (A) -> R.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R> factory(qualifier: Any? = null, generics: Boolean = false) =
        register(InstanceProperty<Unit, (A) -> R>(compoundTypeKey<A, R>(qualifier, generics), Unit))

    /**
     * Creates and registers a property delegate for an optional factory of type (A) -> R.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R> factoryOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(
        InstanceOrNullProperty<Unit, (A) -> R>(
            compoundTypeKey<A, R>(qualifier, generics),
            Unit
        )
    )

    @PublishedApi
    internal fun <T> register(propertyInjector: InjectedProperty<T>): InjectedProperty<T> {
        val injectors = propertyInjectors
            ?: throw IllegalStateException("Injector is already injected.")
        injectors.add(propertyInjector)
        return propertyInjector
    }

    /**
     * Connects the given graph with all previously created property delegates to resolve the
     * dependencies.
     *
     * This can only be called once and it is not possible to create new property delegates after
     * calling this.
     *
     * @param graph The dependency graph to retrieve dependencies from.
     */
    fun inject(graph: Graph) {
        val properties = propertyInjectors
            ?: throw IllegalStateException("Graph was already injected.")
        propertyInjectors = null
        properties.forEach { it.inject(graph) }
    }

    abstract class InjectedProperty<out T> : ReadOnlyProperty<Any?, T> {
        abstract val value: T
        abstract fun inject(graph: Graph)

        fun <R> map(mapper: (T) -> R): InjectedProperty<R> = PropertyMapper(this, mapper)

        final override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return try {
                value
            } catch (e: UninitializedPropertyAccessException) {
                throw WinterException("Injected property `${property.name}` not initialized.")
            }
        }
    }

    @PublishedApi
    internal abstract class AbstractEagerProperty<in A, out R>(
        private val key: TypeKey,
        private val argument: A
    ) : InjectedProperty<R>() {
        private var _value: Any? = UNINITIALIZED_VALUE

        override val value: R
            get() {
                if (_value == UNINITIALIZED_VALUE) {
                    throw UninitializedPropertyAccessException("Property not initialized.")
                }
                @Suppress("UNCHECKED_CAST")
                return _value as R
            }

        override fun inject(graph: Graph) {
            _value = getValue(graph, key, argument)
        }

        protected abstract fun getValue(graph: Graph, key: TypeKey, argument: A): R
    }

    @PublishedApi
    internal abstract class AbstractLazyProperty<A, R>(
        private val key: TypeKey,
        private val argument: A
    ) : InjectedProperty<R>() {
        private var graph: Graph? = null

        private val memorized = memorize {
            val graph = this.graph
                ?: throw UninitializedPropertyAccessException("Property not initialized.")
            getValue(graph, key, argument)
        }

        final override fun inject(graph: Graph) {
            this.graph = graph
            resolveService(graph, key)
        }

        final override val value: R get() = memorized()

        protected open fun resolveService(graph: Graph, key: TypeKey) {
        }

        protected abstract fun getValue(graph: Graph, key: TypeKey, argument: A): R
    }

    @PublishedApi
    internal class PropertyMapper<in I, out O>(
        base: InjectedProperty<I>,
        mapper: (I) -> O
    ) : InjectedProperty<O>() {
        private var base: InjectedProperty<I>? = base
        private var mapper: ((I) -> O)? = mapper
        private val memorized = memorize {
            val fn = this.mapper
                ?: throw IllegalStateException("BUG: PropertyMapper mapper == null")
            val property = this.base
                ?: throw IllegalStateException("BUG: PropertyMapper base == null")
            fn(property.value).also {
                this.base = null
                this.mapper = null
            }
        }

        override val value: O
            get() = memorized()

        override fun inject(graph: Graph) {
            base?.inject(graph)
        }
    }

    @PublishedApi
    internal class ProviderProperty<in A, out R : Any>(
        key: TypeKey,
        argument: A
    ) : AbstractEagerProperty<A, Provider<R>>(key, argument) {

        override fun getValue(graph: Graph, key: TypeKey, argument: A): Provider<R> {
            val service = graph.service<A, R>(key)
            return { service.instance(argument) }
        }

    }

    @PublishedApi
    internal class ProviderOrNullProperty<in A, out R : Any>(
        key: TypeKey,
        argument: A
    ) : AbstractEagerProperty<A, Provider<R>?>(key, argument) {

        override fun getValue(graph: Graph, key: TypeKey, argument: A): Provider<R>? {
            val service = graph.serviceOrNull<A, R>(key) ?: return null
            return { service.instance(argument) }
        }

    }

    @PublishedApi
    internal class InstanceProperty<in A, out R : Any>(
        key: TypeKey,
        argument: A
    ) : AbstractEagerProperty<A, R>(key, argument) {

        override fun getValue(graph: Graph, key: TypeKey, argument: A): R =
            graph.service<A, R>(key).instance(argument)

    }

    @PublishedApi
    internal class InstanceOrNullProperty<in A, out R : Any>(
        key: TypeKey,
        argument: A
    ) : AbstractEagerProperty<A, R?>(key, argument) {

        override fun getValue(graph: Graph, key: TypeKey, argument: A): R? =
            graph.serviceOrNull<A, R>(key)?.instance(argument)

    }

    @PublishedApi
    internal class LazyInstanceProperty<A, R : Any>(
        key: TypeKey,
        argument: A
    ) : AbstractLazyProperty<A, R>(key, argument) {

        private var service: BoundService<A, R>? = null

        override fun resolveService(graph: Graph, key: TypeKey) {
            service = graph.service(key)
        }

        override fun getValue(graph: Graph, key: TypeKey, argument: A): R =
            service!!.instance(argument)
    }

    @PublishedApi
    internal class LazyInstanceOrNullProperty<A, R : Any>(
        key: TypeKey,
        argument: A
    ) : AbstractLazyProperty<A, R?>(key, argument) {

        private var service: BoundService<A, R>? = null

        override fun resolveService(graph: Graph, key: TypeKey) {
            service = graph.serviceOrNull(key)
        }

        override fun getValue(graph: Graph, key: TypeKey, argument: A): R? =
            service?.instance(argument)

    }

    @PublishedApi
    internal class ProvidersOfTypeProperty<out T : Any>(
        key: TypeKey
    ) : AbstractEagerProperty<Unit, Set<Provider<T>>>(key, Unit) {

        override fun getValue(graph: Graph, key: TypeKey, argument: Unit): Set<Provider<T>> =
            graph.providersOfType(key)

    }

    @PublishedApi
    internal class InstancesOfTypeProperty<out T : Any>(
        key: TypeKey
    ) : AbstractEagerProperty<Unit, Set<T>>(key, Unit) {

        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph, key: TypeKey, argument: Unit): Set<T> =
            graph.instancesOfType(key) as Set<T>

    }

    @PublishedApi
    internal class LazyInstancesOfTypeProperty<T : Any>(
        key: TypeKey
    ) : AbstractLazyProperty<Unit, Set<T>>(key, Unit) {

        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph, key: TypeKey, argument: Unit): Set<T> =
            graph.instancesOfType(key) as Set<T>

    }

}
