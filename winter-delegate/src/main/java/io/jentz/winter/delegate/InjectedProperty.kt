package io.jentz.winter.delegate

import io.jentz.winter.*
import io.jentz.winter.services.BoundService
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private val UNINITIALIZED_VALUE = Any()

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

    /**
     * Apply a function to the retrieved instance.
     */
    abstract fun <R> map(mapper: (T) -> R): InjectedProperty<R>

    protected abstract fun doInject(graph: Graph)

    final override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return try {
            value
        } catch (e: UninitializedPropertyAccessException) {
            throw WinterException("Injected property `${property.name}` not initialized.")
        }
    }

}

@PublishedApi
internal class EagerPropertyMapper<in I, out O>(
    base: InjectedProperty<I>,
    mapper: (I) -> O
) : InjectedProperty<O>() {

    private var base: InjectedProperty<I>? = base

    private var mapper: ((I) -> O)? = mapper

    private var _value: Any? = UNINITIALIZED_VALUE

    override val value: O get() {
        if (_value === UNINITIALIZED_VALUE) {
            throw UninitializedPropertyAccessException("Property not initialized.")
        }
        @Suppress("UNCHECKED_CAST")
        return _value as O
    }

    override fun <R> map(mapper: (O) -> R): InjectedProperty<R> =
        EagerPropertyMapper(this, mapper)

    override fun doInject(graph: Graph) {
        val base = this.base!!
        val mapper = this.mapper!!
        base.inject(graph)
        _value = mapper.invoke(base.value)
        this.base = null
        this.mapper = null
    }

}

@PublishedApi
internal class LazyPropertyMapper<in I, out O>(
    base: InjectedProperty<I>,
    mapper: (I) -> O
) : InjectedProperty<O>() {

    private var base: InjectedProperty<I>? = base

    override val value: O by lazy {
        mapper.invoke(base.value)
    }

    override fun <R> map(mapper: (O) -> R): InjectedProperty<R> =
        LazyPropertyMapper(this, mapper)

    override fun doInject(graph: Graph) {
        base!!.inject(graph)
        base = null
    }

}

@PublishedApi
internal abstract class AbstractEagerProperty<R : Any?, T>(
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

    override fun <R> map(mapper: (T) -> R): InjectedProperty<R> =
        EagerPropertyMapper(this, mapper)

    override fun doInject(graph: Graph) {
        _value = getValue(graph, key)
    }

    protected abstract fun getValue(graph: Graph, key: TypeKey<R>): T

}

@PublishedApi
internal abstract class AbstractLazyProperty<R : Any?, T>(
    val key: TypeKey<R>
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

    override fun <R> map(mapper: (T) -> R): InjectedProperty<R> =
        LazyPropertyMapper(this, mapper)

    override fun doInject(graph: Graph) {
        service = resolveService(graph, key)
    }

    protected abstract fun resolveService(graph: Graph, key: TypeKey<R>): BoundService<R>?

    protected abstract fun getValue(service: BoundService<R>?): T

}

@PublishedApi
internal class InstanceProperty<R : Any?>(
    key: TypeKey<R>,
    private val block: ComponentBuilderBlock?
) : AbstractEagerProperty<R, R>(key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): R =
        graph.instanceByKey(key, block)

}

@PublishedApi
internal class LazyInstanceProperty<R : Any?>(
    key: TypeKey<R>,
    private val block: ComponentBuilderBlock?
) : AbstractLazyProperty<R, R>(key) {

    override fun resolveService(graph: Graph, key: TypeKey<R>): BoundService<R>? {
        val service = graph.service(key)
        if (service == null && !key.isOptional)
            throw EntryNotFoundException(key)
        return graph.service(key)
    }

    override fun getValue(service: BoundService<R>?): R {
        @Suppress("UNCHECKED_CAST")
        return if (key.isOptional) service?.instance(block) as R
        else service?.instance(block) ?: throw EntryNotFoundException(key)
    }
}

@PublishedApi
internal class ProviderProperty<R : Any>(
    key: TypeKey<R>,
    private val block: ComponentBuilderBlock?
) : AbstractEagerProperty<R, Provider<R>>(key) {

    override fun getValue(graph: Graph, key: TypeKey<R>): Provider<R> =
        graph.providerByKey(key, block)

}
