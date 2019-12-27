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
    ) = register(ProviderProperty(typeKey<R>(qualifier, generics)))

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
    ) = register(ProviderOrNullProperty(typeKey<R>(qualifier, generics)))

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
    ) = register(InstanceProperty(typeKey<R>(qualifier, generics)))

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
    ) = register(InstanceOrNullProperty(typeKey<R>(qualifier, generics)))

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
    ) = register(LazyInstanceProperty(typeKey<R>(qualifier, generics)))

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
    ) = register(LazyInstanceOrNullProperty(typeKey<R>(qualifier, generics)))

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
    internal abstract class AbstractEagerProperty<R : Any, T>(
        private val key: TypeKey<R>
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

        protected abstract fun getValue(graph: Graph, key: TypeKey<R>): T

    }

    @PublishedApi
    internal abstract class AbstractLazyProperty<R : Any, T>(
        private val key: TypeKey<R>
    ) : InjectedProperty<T>() {

        private var graph: Graph? = null

        final override val value: T by lazy {
            val graph = this.graph
                ?: throw UninitializedPropertyAccessException("Property not initialized.")

            getValue(graph, key)
        }

        final override fun inject(graph: Graph) {
            this.graph = graph
            resolveFactory(graph, key)
        }

        protected open fun resolveFactory(graph: Graph, key: TypeKey<R>) {
        }

        protected abstract fun getValue(graph: Graph, key: TypeKey<R>): T

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
    internal class InstanceProperty<R : Any>(
        key: TypeKey<R>
    ) : AbstractEagerProperty<R, R>(key) {

        override fun getValue(graph: Graph, key: TypeKey<R>): R =
            graph.instanceByKey(key)

    }

    @PublishedApi
    internal class InstanceOrNullProperty<R : Any>(
        key: TypeKey<R>
    ) : AbstractEagerProperty<R, R?>(key) {

        override fun getValue(graph: Graph, key: TypeKey<R>): R? =
            graph.instanceOrNullByKey(key)

    }

    @PublishedApi
    internal class LazyInstanceProperty<R : Any>(
        key: TypeKey<R>
    ) : AbstractLazyProperty<R, R>(key) {

        private lateinit var provider: Provider<R>

        override fun resolveFactory(graph: Graph, key: TypeKey<R>) {
            provider = graph.providerByKey(key)
        }

        override fun getValue(graph: Graph, key: TypeKey<R>): R = provider()

    }

    @PublishedApi
    internal class LazyInstanceOrNullProperty<R : Any>(
        key: TypeKey<R>
    ) : AbstractLazyProperty<R, R?>(key) {

        private var provider: Provider<R>? = null

        override fun resolveFactory(graph: Graph, key: TypeKey<R>) {
            provider = graph.providerOrNullByKey(key)
        }

        override fun getValue(graph: Graph, key: TypeKey<R>): R? =
            provider?.invoke()

    }

    @PublishedApi
    internal class ProviderProperty<R : Any>(
        key: TypeKey<R>
    ) : AbstractEagerProperty<R, Provider<R>>(key) {

        override fun getValue(graph: Graph, key: TypeKey<R>): Provider<R> =
            graph.providerByKey(key)

    }

    @PublishedApi
    internal class ProviderOrNullProperty<R : Any>(
        key: TypeKey<R>
    ) : AbstractEagerProperty<R, Provider<R>?>(key) {

        override fun getValue(graph: Graph, key: TypeKey<R>): Provider<R>? =
            graph.providerOrNullByKey(key)

    }

    @PublishedApi
    internal class ProvidersOfTypeProperty<R : Any>(
        key: TypeKey<R>
    ) : AbstractEagerProperty<R, Set<Provider<R>>>(key) {

        override fun getValue(graph: Graph, key: TypeKey<R>): Set<Provider<R>> =
            graph.providersOfTypeByKey(key)

    }

    @PublishedApi
    internal class InstancesOfTypeProperty<R : Any>(
        key: TypeKey<R>
    ) : AbstractEagerProperty<R, Set<R>>(key) {

        override fun getValue(graph: Graph, key: TypeKey<R>): Set<R> =
            graph.instancesOfTypeByKey(key)

    }

    @PublishedApi
    internal class LazyInstancesOfTypeProperty<R : Any>(
        key: TypeKey<R>
    ) : AbstractLazyProperty<R, Set<R>>(key) {

        override fun getValue(graph: Graph, key: TypeKey<R>): Set<R> =
            graph.instancesOfTypeByKey(key)

    }

}
