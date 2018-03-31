package io.jentz.winter

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * The Injector provides a was to inject dependencies into properties via Kotlins property delegates.
 *
 * E.g.
 * ```
 * class MyClass {
 *   private val injector = Injector()
 *   private val eagerService: Service1 by injector.instance()
 *   private val lazyService: Service2 by injector.lazyInstance()
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
     * Creates and registers a property delegate for a [Provider] of type `T`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> provider(qualifier: Any? = null, generics: Boolean = false) =
            register(ProviderProperty<T>(typeKey<T>(qualifier, generics)))

    /**
     * Creates and registers a property delegate for an optional [Provider] of type `T`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any?> providerOrNull(qualifier: Any? = null, generics: Boolean = false) =
            register(ProviderOrNullProperty<T>(typeKey<T>(qualifier, generics)))

    /**
     * Creates and registers a property delegate for an instance of type `T`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> instance(qualifier: Any? = null, generics: Boolean = false) =
            register(InstanceProperty<T>(typeKey<T>(qualifier, generics)))

    /**
     * Creates and registers a property delegate for an optional instance of type `T`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any?> instanceOrNull(qualifier: Any? = null, generics: Boolean = false) =
            register(InstanceOrNullProperty<T>(typeKey<T>(qualifier, generics)))

    /**
     * Creates and registers a lazy property delegate for an instance of type `T`.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any> lazyInstance(qualifier: Any? = null, generics: Boolean = false) =
            register(LazyInstanceProperty<T>(typeKey<T>(qualifier, generics)))

    /**
     * Creates and registers a lazy property delegate for an optional instance of type `T`.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified T : Any?> lazyInstanceOrNull(qualifier: Any? = null, generics: Boolean = false) =
            register(LazyInstanceOrNullProperty<T>(typeKey<T>(qualifier, generics)))

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
            register(InstanceProperty<(A) -> R>(compoundTypeKey<A, R>(qualifier, generics)))

    /**
     * Creates and registers a property delegate for an optional factory of type (A) -> R.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @return The created [InjectedProperty].
     */
    inline fun <reified A, reified R> factoryOrNull(qualifier: Any? = null, generics: Boolean = false) =
            register(InstanceOrNullProperty<(A) -> R>(compoundTypeKey<A, R>(qualifier, generics)))

    @PublishedApi
    internal fun <T> register(propertyInjector: InjectedProperty<T>): InjectedProperty<T> {
        val injectors = propertyInjectors ?: throw IllegalStateException("Injector is already injected.")
        injectors.add(propertyInjector)
        return propertyInjector
    }

    fun inject(graph: Graph) {
        propertyInjectors?.forEach { it.inject(graph) }
        propertyInjectors = null
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
    internal abstract class AbstractEagerProperty<out T> : InjectedProperty<T>() {
        private var _value: Any? = UNINITIALIZED_VALUE

        override val value: T
            get() {
                if (_value == UNINITIALIZED_VALUE) {
                    throw UninitializedPropertyAccessException("Property not initialized.")
                }
                @Suppress("UNCHECKED_CAST")
                return _value as T
            }

        override fun inject(graph: Graph) {
            _value = getValue(graph)
        }

        abstract protected fun getValue(graph: Graph): T
    }

    @PublishedApi
    internal abstract class AbstractLazyProperty<out T> : InjectedProperty<T>() {
        private var graph: Graph? = null
        private val memorized = memorize {
            val graph = graph ?: throw UninitializedPropertyAccessException("Property not initialized.")
            this.graph = null
            getValue(graph)
        }

        override fun inject(graph: Graph) {
            this.graph = graph
        }

        override val value: T get() = memorized()

        abstract protected fun getValue(graph: Graph): T
    }

    @PublishedApi
    internal class PropertyMapper<in I, out O>(base: InjectedProperty<I>, mapper: (I) -> O) : InjectedProperty<O>() {
        private var base: InjectedProperty<I>? = base
        private var mapper: ((I) -> O)? = mapper
        private val memorized = memorize {
            val fn = this.mapper ?: throw IllegalStateException("BUG: PropertyMapper mapper == null")
            val property = this.base ?: throw IllegalStateException("BUG: PropertyMapper base == null")
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
    internal class ProviderProperty<out T : Any>(private val key: DependencyKey) : AbstractEagerProperty<Provider<T>>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): Provider<T> = graph.provider(key) as Provider<T>
    }

    @PublishedApi
    internal class ProviderOrNullProperty<out T : Any?>(private val key: DependencyKey) : AbstractEagerProperty<Provider<T>?>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): Provider<T>? = graph.providerOrNull(key) as? Provider<T>
    }

    @PublishedApi
    internal class InstanceProperty<out T : Any>(private val key: DependencyKey) : AbstractEagerProperty<T>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T = graph.provider(key).invoke() as T
    }

    @PublishedApi
    internal class InstanceOrNullProperty<out T : Any?>(private val key: DependencyKey) : AbstractEagerProperty<T?>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T? = graph.providerOrNull(key)?.invoke() as? T
    }

    @PublishedApi
    internal class LazyInstanceProperty<out T : Any>(private val key: DependencyKey) : AbstractLazyProperty<T>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T = graph.provider(key).invoke() as T
    }

    @PublishedApi
    internal class LazyInstanceOrNullProperty<out T : Any?>(private val key: DependencyKey) : AbstractLazyProperty<T?>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T? = graph.providerOrNull(key)?.invoke() as? T
    }

    @PublishedApi
    internal class ProvidersOfTypeProperty<out T : Any>(private val key: DependencyKey) : AbstractEagerProperty<Set<Provider<T>>>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): Set<Provider<T>> = graph.providersOfType(key) as Set<Provider<T>>
    }

    @PublishedApi
    internal class InstancesOfTypeProperty<out T : Any>(private val key: DependencyKey) : AbstractEagerProperty<Set<T>>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): Set<T> = graph.instancesOfType(key) as Set<T>
    }

    @PublishedApi
    internal class LazyInstancesOfTypeProperty<out T : Any>(private val key: DependencyKey) : AbstractLazyProperty<Set<T>>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): Set<T> = graph.instancesOfType(key) as Set<T>
    }

}