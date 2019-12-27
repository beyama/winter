package io.jentz.winter.delegate

import io.jentz.winter.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class InjectedProperty<out T>(
    private val application: WinterApplication
) : ReadOnlyProperty<Any?, T> {

    abstract val value: T
    abstract fun inject(graph: Graph)

    fun <R> map(mapper: (T) -> R): InjectedProperty<R> =
        PropertyMapper(application,this, mapper)

    operator fun provideDelegate(thisRef: Any, prop: KProperty<*>): InjectedProperty<T> {
        application.delegateNotifier.register(thisRef, this)
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
    application: WinterApplication,
    base: InjectedProperty<I>,
    mapper: (I) -> O
) : InjectedProperty<O>(application) {

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
    application: WinterApplication,
    private val key: TypeKey<R>
) : InjectedProperty<T>(application) {

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
    application: WinterApplication,
    private val key: TypeKey<R>
) : InjectedProperty<T>(application) {

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
    application: WinterApplication,
    key: TypeKey<R>
) : AbstractEagerProperty<R, R>(application, key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): R =
        graph.instanceByKey(key)

}

@PublishedApi
internal class InstanceOrNullProperty<R : Any>(
    application: WinterApplication,
    key: TypeKey<R>
) : AbstractEagerProperty<R, R?>(application, key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): R? =
        graph.instanceOrNullByKey(key)

}

@PublishedApi
internal class LazyInstanceProperty<R : Any>(
    application: WinterApplication,
    key: TypeKey<R>
) : AbstractLazyProperty<R, R>(application, key) {

    private lateinit var provider: Provider<R>

    override fun resolveFactory(graph: Graph, key: TypeKey<R>) {
        provider = graph.providerByKey(key)
    }

    override fun getValue(graph: Graph, key: TypeKey<R>): R = provider()

}

@PublishedApi
internal class LazyInstanceOrNullProperty<R : Any>(
    application: WinterApplication,
    key: TypeKey<R>
) : AbstractLazyProperty<R, R?>(application, key) {

    private var provider: Provider<R>? = null

    override fun resolveFactory(graph: Graph, key: TypeKey<R>) {
        provider = graph.providerOrNullByKey(key)
    }

    override fun getValue(graph: Graph, key: TypeKey<R>): R? =
        provider?.invoke()

}

@PublishedApi
internal class ProviderProperty<R : Any>(
    application: WinterApplication,
    key: TypeKey<R>
) : AbstractEagerProperty<R, Provider<R>>(application, key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): Provider<R> =
        graph.providerByKey(key)

}

@PublishedApi
internal class ProviderOrNullProperty<R : Any>(
    application: WinterApplication,
    key: TypeKey<R>
) : AbstractEagerProperty<R, Provider<R>?>(application, key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): Provider<R>? =
        graph.providerOrNullByKey(key)

}

@PublishedApi
internal class ProvidersOfTypeProperty<R : Any>(
    application: WinterApplication,
    key: TypeKey<R>
) : AbstractEagerProperty<R, Set<Provider<R>>>(application, key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): Set<Provider<R>> =
        graph.providersOfTypeByKey(key)

}

@PublishedApi
internal class InstancesOfTypeProperty<R : Any>(
    application: WinterApplication,
    key: TypeKey<R>
) : AbstractEagerProperty<R, Set<R>>(application, key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): Set<R> =
        graph.instancesOfTypeByKey(key)

}

@PublishedApi
internal class LazyInstancesOfTypeProperty<R : Any>(
    application: WinterApplication,
    key: TypeKey<R>
) : AbstractLazyProperty<R, Set<R>>(application, key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): Set<R> =
        graph.instancesOfTypeByKey(key)

}
