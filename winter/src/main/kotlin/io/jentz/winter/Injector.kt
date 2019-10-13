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
     * Creates and registers a property delegate for a [Provider] of type `() -> R`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> provider(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(ProviderProperty(typeKey<R>(qualifier, generics), Unit))

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
    ) = register(ProviderProperty(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a property delegate for an optional [Provider] of type `R`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> providerOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(ProviderOrNullProperty(typeKey<R>(qualifier, generics), Unit))

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
    ) = register(ProviderOrNullProperty(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a property delegate for an instance of type `R`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> instance(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(InstanceProperty(typeKey<R>(qualifier, generics), Unit))

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
    ) = register(InstanceProperty(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a property delegate for an optional instance of type `R`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> instanceOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(InstanceOrNullProperty(typeKey<R>(qualifier, generics), Unit))

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
    ) = register(InstanceOrNullProperty(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a lazy property delegate for an instance of type `R`.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> lazyInstance(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(LazyInstanceProperty(typeKey<R>(qualifier, generics), Unit))

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
    ) = register(LazyInstanceProperty(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a lazy property delegate for an optional instance of type `R`.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> lazyInstanceOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(LazyInstanceOrNullProperty(typeKey<R>(qualifier, generics), Unit))

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
    ) = register(LazyInstanceOrNullProperty(compoundTypeKey<A, R>(qualifier, generics), argument))

    /**
     * Creates and registers a property delegate for a [Set] of [providers][Provider] of type `R`.
     *
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> providersOfType(generics: Boolean = false) =
        register(ProvidersOfTypeProperty(typeKeyOfType<R>(generics)))

    /**
     * Creates and registers a property delegate for a [Set] of instances of type `R`.
     *
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> instancesOfType(generics: Boolean = false) =
        register(InstancesOfTypeProperty(typeKeyOfType<R>(generics)))

    /**
     * Creates and registers a lazy property delegate for a [Set] of instances of type `R`.
     *
     * The instances get retrieved/created on first property access.
     *
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> lazyInstancesOfType(generics: Boolean = false) =
        register(LazyInstancesOfTypeProperty(typeKeyOfType<R>(generics)))

    /**
     * Creates and registers a property delegate for a factory of type (A) -> R.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R : Any> factory(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(FactoryProperty(compoundTypeKey<A, R>(qualifier, generics)))

    /**
     * Creates and registers a property delegate for an optional factory of type (A) -> R.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R : Any> factoryOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
    ) = register(FactoryOrNullProperty(compoundTypeKey<A, R>(qualifier, generics)))

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
    internal abstract class AbstractEagerProperty<A, R : Any, T>(
        private val key: TypeKey<A, R>
    ) : InjectedProperty<T>() {

        private var _value: Any? = UNINITIALIZED_VALUE

        final override val value: T
            get() {
                if (_value == UNINITIALIZED_VALUE) {
                    throw UninitializedPropertyAccessException("Property not initialized.")
                }
                @Suppress("UNCHECKED_CAST")
                return _value as T
            }

        final override fun inject(graph: Graph) {
            _value = getValue(graph, key)
        }

        protected abstract fun getValue(graph: Graph, key: TypeKey<A, R>): T

    }

    @PublishedApi
    internal abstract class AbstractLazyProperty<A, R : Any, T>(
        private val key: TypeKey<A, R>,
        private val argument: A
    ) : InjectedProperty<T>() {

        private var graph: Graph? = null

        final override val value: T by lazy {
            val graph = this.graph
                ?: throw UninitializedPropertyAccessException("Property not initialized.")

            getValue(graph, key, argument)
        }

        final override fun inject(graph: Graph) {
            this.graph = graph
            resolveFactory(graph, key)
        }

        protected open fun resolveFactory(graph: Graph, key: TypeKey<A, R>) {
        }

        protected abstract fun getValue(graph: Graph, key: TypeKey<A, R>, argument: A): T

    }

    @PublishedApi
    internal class PropertyMapper<in I, out O>(
        base: InjectedProperty<I>,
        mapper: (I) -> O
    ) : InjectedProperty<O>() {

        private var base: InjectedProperty<I>? = base

        private var mapper: ((I) -> O)? = mapper

        override val value: O by lazy {
            val fn = this.mapper
                ?: throw IllegalStateException("BUG: PropertyMapper mapper == null")

            val property = this.base
                ?: throw IllegalStateException("BUG: PropertyMapper base == null")

            this.base = null
            this.mapper = null

            fn(property.value)
        }

        override fun inject(graph: Graph) {
            base?.inject(graph)
        }

    }

    @PublishedApi
    internal class InstanceProperty<A, R : Any>(
        key: TypeKey<A, R>,
        private val argument: A
    ) : AbstractEagerProperty<A, R, R>(key) {

        override fun getValue(graph: Graph, key: TypeKey<A, R>): R =
            graph.instanceByKey(key, argument)

    }

    @PublishedApi
    internal class InstanceOrNullProperty<A, R : Any>(
        key: TypeKey<A, R>,
        private val argument: A
    ) : AbstractEagerProperty<A, R, R?>(key) {

        override fun getValue(graph: Graph, key: TypeKey<A, R>): R? =
            graph.instanceOrNullByKey(key, argument)

    }

    @PublishedApi
    internal class LazyInstanceProperty<A, R : Any>(
        key: TypeKey<A, R>,
        argument: A
    ) : AbstractLazyProperty<A, R, R>(key, argument) {

        private var factory: Factory<A, R>? = null

        override fun resolveFactory(graph: Graph, key: TypeKey<A, R>) {
            factory = graph.factoryByKey(key)
        }

        override fun getValue(graph: Graph, key: TypeKey<A, R>, argument: A): R =
            factory!!.invoke(argument)

    }

    @PublishedApi
    internal class LazyInstanceOrNullProperty<A, R : Any>(
        key: TypeKey<A, R>,
        argument: A
    ) : AbstractLazyProperty<A, R, R?>(key, argument) {

        private var factory: Factory<A, R>? = null

        override fun resolveFactory(graph: Graph, key: TypeKey<A, R>) {
            factory = graph.factoryOrNullByKey(key)
        }

        override fun getValue(graph: Graph, key: TypeKey<A, R>, argument: A): R? =
            factory?.invoke(argument)

    }

    @PublishedApi
    internal class ProviderProperty<A, R : Any>(
        key: TypeKey<A, R>,
        private val argument: A
    ) : AbstractEagerProperty<A, R, Provider<R>>(key) {

        override fun getValue(graph: Graph, key: TypeKey<A, R>): Provider<R> =
            graph.providerByKey(key, argument)

    }

    @PublishedApi
    internal class ProviderOrNullProperty<A, R : Any>(
        key: TypeKey<A, R>,
        private val argument: A
    ) : AbstractEagerProperty<A, R, Provider<R>?>(key) {

        override fun getValue(graph: Graph, key: TypeKey<A, R>): Provider<R>? =
            graph.providerOrNullByKey(key, argument)

    }

    @PublishedApi
    internal class FactoryProperty<A, R : Any>(
        key: TypeKey<A, R>
    ) : AbstractEagerProperty<A, R, Factory<A, R>>(key) {

        override fun getValue(graph: Graph, key: TypeKey<A, R>): Factory<A, R> =
            graph.factoryByKey(key)

    }

    @PublishedApi
    internal class FactoryOrNullProperty<A, R : Any>(
        key: TypeKey<A, R>
    ) : AbstractEagerProperty<A, R, Factory<A, R>?>(key) {

        override fun getValue(graph: Graph, key: TypeKey<A, R>): Factory<A, R>? =
            graph.factoryOrNullByKey(key)

    }

    @PublishedApi
    internal class ProvidersOfTypeProperty<R : Any>(
        key: TypeKey<Unit, R>
    ) : AbstractEagerProperty<Unit, R, Set<Provider<R>>>(key) {

        override fun getValue(graph: Graph, key: TypeKey<Unit, R>): Set<Provider<R>> =
            graph.providersOfTypeByKey(key)

    }

    @PublishedApi
    internal class InstancesOfTypeProperty<R : Any>(
        key: TypeKey<Unit, R>
    ) : AbstractEagerProperty<Unit, R, Set<R>>(key) {

        override fun getValue(graph: Graph, key: TypeKey<Unit, R>): Set<R> =
            graph.instancesOfTypeByKey(key)

    }

    @PublishedApi
    internal class LazyInstancesOfTypeProperty<R : Any>(
        key: TypeKey<Unit, R>
    ) : AbstractLazyProperty<Unit, R, Set<R>>(key, Unit) {

        override fun getValue(graph: Graph, key: TypeKey<Unit, R>, argument: Unit): Set<R> =
            graph.instancesOfTypeByKey(key)

    }

}
