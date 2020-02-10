package io.jentz.winter.delegate

import io.jentz.winter.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Creates a property delegate for a [Provider] of type `() -> R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectProvider(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<Provider<R>> = ProviderProperty(typeKey(qualifier, generics))

/**
 * Creates a property delegate for an optional [Provider] of type `R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectProviderOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<Provider<R>?> = ProviderOrNullProperty(typeKey(qualifier, generics))

/**
 * Creates a property delegate for an instance of type `R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> inject(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<R> = InstanceProperty(typeKey(qualifier, generics))

/**
 * Creates a property delegate for an optional instance of type `R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<R?> = InstanceOrNullProperty(typeKey(qualifier, generics))

/**
 * Creates a lazy property delegate for an instance of type `R`.
 *
 * The instance gets retrieved/created on first property access.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectLazy(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<R> = LazyInstanceProperty(typeKey(qualifier, generics))

/**
 * Creates a lazy property delegate for an optional instance of type `R`.
 *
 * The instance gets retrieved/created on first property access.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> injectLazyOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<R?> = LazyInstanceOrNullProperty(typeKey(qualifier, generics))

/**
 * Base class of all injected properties.
 */
abstract class InjectedProperty<out T> : ReadOnlyProperty<Any?, T> {

    abstract val value: T
    abstract fun inject(graph: Graph)

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
