package io.jentz.winter.delegate

import io.jentz.winter.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Creates a property delegate for a [Provider] of type `() -> R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectProvider(
    qualifier: Any? = null,
    generics: Boolean = false,
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<Provider<R>> = ProviderProperty(typeKey(qualifier, generics), block)

/**
 * Creates a property delegate for an optional [Provider] of type `R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectProviderOrNull(
    qualifier: Any? = null,
    generics: Boolean = false,
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<Provider<R>?> = ProviderOrNullProperty(typeKey(qualifier, generics), block)

/**
 * Creates a property delegate for an instance of type `R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> inject(
    qualifier: Any? = null,
    generics: Boolean = false,
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<R> = InstanceProperty(typeKey(qualifier, generics), block)

/**
 * Creates a property delegate for an optional instance of type `R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectOrNull(
    qualifier: Any? = null,
    generics: Boolean = false,
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<R?> = InstanceOrNullProperty(typeKey(qualifier, generics), block)

/**
 * Creates a lazy property delegate for an instance of type `R`.
 *
 * The instance gets retrieved/created on first property access.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectLazy(
    qualifier: Any? = null,
    generics: Boolean = false,
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<R> = LazyInstanceProperty(typeKey(qualifier, generics), block)

/**
 * Creates a lazy property delegate for an optional instance of type `R`.
 *
 * The instance gets retrieved/created on first property access.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectLazyOrNull(
    qualifier: Any? = null,
    generics: Boolean = false,
    noinline block: ComponentBuilderBlock? = null
): InjectedProperty<R?> = LazyInstanceOrNullProperty(typeKey(qualifier, generics), block)

/**
 * Base class of all injected properties.
 */
abstract class InjectedProperty<out T> : ReadOnlyProperty<Any?, T> {

    @Volatile var isInjected = false
        private set

    abstract val value: T

    fun inject(graph: Graph) {
        if (isInjected) throw WinterException("Inject was called multiple times.")
        doInject(graph)
        isInjected = true
    }

    protected abstract fun doInject(graph: Graph)

    /**
     * Apply a function to the retrieved instance.
     */
    fun <R> map(mapper: (T) -> R): InjectedProperty<R> =
        PropertyMapper(this, mapper)

    operator fun provideDelegate(thisRef: Any, prop: KProperty<*>): InjectedProperty<T> {
        DelegateNotifier.register(thisRef, this)
        return this
    }

    final override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return try {
            value
        } catch (e: UninitializedPropertyAccessException) {
            throw WinterException("Injected property `${property.name}` not initialized.")
        }
    }

}

@PublishedApi
internal class PropertyMapper<in I, out O>(
    base: InjectedProperty<I>,
    mapper: (I) -> O
) : InjectedProperty<O>() {

    private var base: InjectedProperty<I>? = base

    private var mapper: ((I) -> O)? = mapper

    override val value: O by lazy {
        val fn = checkNotNull(this.mapper) { "BUG: PropertyMapper mapper == null" }
        val property = checkNotNull(this.base) { "BUG: PropertyMapper base == null" }

        fn(property.value).also {
            this.base = null
            this.mapper = null
        }
    }

    override fun doInject(graph: Graph) {
        base!!.inject(graph)
    }

}

@PublishedApi
internal abstract class AbstractEagerProperty<R : Any, T>(
    private val key: TypeKey<R>
) : InjectedProperty<T>() {

    private var _value: Any? = UNINITIALIZED_VALUE

    final override val value: T
        get() {
            if (_value === UNINITIALIZED_VALUE) {
                throw UninitializedPropertyAccessException("Property not initialized.")
            }
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun doInject(graph: Graph) {
        _value = getValue(graph, key)
    }

    protected abstract fun getValue(graph: Graph, key: TypeKey<R>): T

}

@PublishedApi
internal abstract class AbstractLazyProperty<R : Any, T>(
    private val key: TypeKey<R>
) : InjectedProperty<T>() {

    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    private var service: BoundService<R>? = null

    @Suppress("UNCHECKED_CAST")
    final override val value: T
        get() {
            if (!isInjected) throw UninitializedPropertyAccessException("Property not initialized.")

            val v1 = _value
            if (v1 !== UNINITIALIZED_VALUE) return v1 as T

            synchronized(this) {
                val v2 = _value
                if (v2 !== UNINITIALIZED_VALUE) return v2 as T

                return getValue(service).also { _value = it }
            }
        }

    override fun doInject(graph: Graph) {
        service = resolveService(graph, key)
    }

    protected abstract fun resolveService(graph: Graph, key: TypeKey<R>): BoundService<R>?

    protected abstract fun getValue(service: BoundService<R>?): T

}

@PublishedApi
internal class InstanceProperty<R : Any>(
    key: TypeKey<R>,
    private val block: ComponentBuilderBlock?
) : AbstractEagerProperty<R, R>(key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): R =
        graph.instanceByKey(key, block)

}

@PublishedApi
internal class InstanceOrNullProperty<R : Any>(
    key: TypeKey<R>,
    private val block: ComponentBuilderBlock?
) : AbstractEagerProperty<R, R?>(key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): R? =
        graph.instanceOrNullByKey(key, block)

}

@PublishedApi
internal class LazyInstanceProperty<R : Any>(
    key: TypeKey<R>,
    private val block: ComponentBuilderBlock?
) : AbstractLazyProperty<R, R>(key) {

    override fun resolveService(graph: Graph, key: TypeKey<R>) = graph.service(key)

    override fun getValue(service: BoundService<R>?): R {
        service ?: throw UninitializedPropertyAccessException("Service was not resolved.")
        return service.instance(block)
    }
}

@PublishedApi
internal class LazyInstanceOrNullProperty<R : Any>(
    key: TypeKey<R>,
    private val block: ComponentBuilderBlock?
) : AbstractLazyProperty<R, R?>(key) {

    override fun resolveService(graph: Graph, key: TypeKey<R>) = graph.serviceOrNull(key)

    override fun getValue(service: BoundService<R>?): R? = service?.instance(block)

}

@PublishedApi
internal class ProviderProperty<R : Any>(
    key: TypeKey<R>,
    private val block: ComponentBuilderBlock?
) : AbstractEagerProperty<R, Provider<R>>(key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): Provider<R> =
        graph.providerByKey(key, block)

}

@PublishedApi
internal class ProviderOrNullProperty<R : Any>(
    key: TypeKey<R>,
    private val block: ComponentBuilderBlock?
) : AbstractEagerProperty<R, Provider<R>?>(key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): Provider<R>? =
        graph.providerOrNullByKey(key, block)

}
