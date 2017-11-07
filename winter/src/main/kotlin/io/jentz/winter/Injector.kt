package io.jentz.winter

import io.jentz.winter.internal.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Injector {
    private val propertyInjectors = mutableListOf<PropertyInjector<*>>()

    interface InjectedProperty<out T> : ReadOnlyProperty<Any?, T> {
        fun <R> map(mapper: (T) -> R): InjectedProperty<R>
    }

    interface PropertyInjector<out T> : InjectedProperty<T> {
        fun inject(graph: Graph)
    }

    abstract class AbstractEagerProperty<out T> : PropertyInjector<T> {
        private var value: Any? = UNINITIALIZED_VALUE

        override fun inject(graph: Graph) {
            value = getValue(graph)
        }

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            if (value == UNINITIALIZED_VALUE) {
                throw WinterException("Injected property `${property.name}` not initialized.")
            }
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        abstract fun getValue(graph: Graph): T

        final override fun <R> map(mapper: (T) -> R): InjectedProperty<R> = MapProperty(this, mapper)
    }

    abstract class AbstractLazyProperty<out T> : PropertyInjector<T> {
        private var graph: Graph? = null
        private val memorized = memorize {
            val graph = graph ?: throw WinterException("Injector graph must be set before accessing injected properties.")
            getValue(graph)
        }

        override fun inject(graph: Graph) {
            this.graph = graph
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>) = memorized()

        abstract fun getValue(graph: Graph): T

        final override fun <R> map(mapper: (T) -> R): InjectedProperty<R> = MapProperty(this, mapper)
    }

    private class MapProperty<in I, out O>(private val base: InjectedProperty<I>, private val mapper: (I) -> O) : InjectedProperty<O> {
        private var value: Any? = UNINITIALIZED_VALUE

        override fun getValue(thisRef: Any?, property: KProperty<*>): O {
            val v1 = value
            @Suppress("UNCHECKED_CAST")
            if (v1 !== UNINITIALIZED_VALUE) return v1 as O

            synchronized(this) {
                val v2 = value
                @Suppress("UNCHECKED_CAST")
                if (v2 !== UNINITIALIZED_VALUE) return v2 as O

                val typedValue = mapper(base.getValue(thisRef, property))
                value = typedValue
                return typedValue
            }
        }

        override fun <R> map(mapper: (O) -> R): InjectedProperty<R> = MapProperty(this, mapper)
    }

    class Instance<out T : Any>(private val key: DependencyKey) : AbstractEagerProperty<T>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T = graph.provider(key).invoke() as T
    }

    class InstanceOrNull<out T : Any?>(private val key: DependencyKey) : AbstractEagerProperty<T?>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T? = graph.providerOrNull(key)?.invoke() as? T
    }

    class LazyInstance<out T : Any>(private val key: DependencyKey) : AbstractLazyProperty<T>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T = graph.provider(key).invoke() as T
    }

    class LazyInstanceOrNull<out T : Any?>(private val key: DependencyKey) : AbstractLazyProperty<T?>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T? = graph.providerOrNull(key)?.invoke() as? T
    }

    inline fun <reified T : Any> instance(qualifier: Any? = null, generics: Boolean = false)
            = register(Instance<T>(if (generics) genericTypeKey<T>(qualifier) else typeKey<T>(qualifier)))

    inline fun <reified T : Any?> instanceOrNull(qualifier: Any? = null, generics: Boolean = false)
            = register(InstanceOrNull<T>(if (generics) genericTypeKey<T>(qualifier) else typeKey<T>(qualifier)))

    inline fun <reified T : Any> lazyInstance(qualifier: Any? = null, generics: Boolean = false)
            = register(LazyInstance<T>(if (generics) genericTypeKey<T>(qualifier) else typeKey<T>(qualifier)))

    inline fun <reified T : Any?> lazyInstanceOrNull(qualifier: Any? = null, generics: Boolean = false)
            = register(LazyInstanceOrNull<T>(if (generics) genericTypeKey<T>(qualifier) else typeKey<T>(qualifier)))

    inline fun <reified A, reified R> factory(qualifier: Any? = null, generics: Boolean = false)
            = register(Instance<(A) -> R>(if (generics) genericCompoundTypeKey<A, R>(qualifier) else compoundTypeKey<A, R>(qualifier)))

    inline fun <reified A, reified R> factoryOrNull(qualifier: Any? = null, generics: Boolean = false)
            = register(InstanceOrNull<(A) -> R>(if (generics) genericCompoundTypeKey<A, R>(qualifier) else compoundTypeKey<A, R>(qualifier)))

    fun <T> register(propertyInjector: PropertyInjector<T>): InjectedProperty<T> {
        propertyInjectors.add(propertyInjector)
        return propertyInjector
    }

    fun inject(graph: Graph) {
        propertyInjectors.forEach { it.inject(graph) }
        propertyInjectors.clear()
    }

}