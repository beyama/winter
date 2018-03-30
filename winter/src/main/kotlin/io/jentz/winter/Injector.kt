package io.jentz.winter

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Injector {
    private val propertyInjectors = mutableListOf<InjectedProperty<*>>()

    var injected = false
        private set

    abstract class InjectedProperty<out T> : ReadOnlyProperty<Any?, T> {
        abstract val value: T
        abstract fun inject(graph: Graph)

        fun <R> map(mapper: (T) -> R): InjectedProperty<R> = MapProperty(this, mapper)

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
    internal class MapProperty<in I, out O>(base: InjectedProperty<I>, mapper: (I) -> O) : InjectedProperty<O>() {
        private var base: InjectedProperty<I>? = base
        private var mapper: ((I) -> O)? = mapper
        private val memorized = memorize {
            val fn = this.mapper ?: throw IllegalStateException("BUG: MapProperty mapper == null")
            val property = this.base ?: throw IllegalStateException("BUG: MapProperty base == null")
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
    internal class Instance<out T : Any>(private val key: DependencyKey) : AbstractEagerProperty<T>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T = graph.provider(key).invoke() as T
    }

    @PublishedApi
    internal class InstanceOrNull<out T : Any?>(private val key: DependencyKey) : AbstractEagerProperty<T?>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T? = graph.providerOrNull(key)?.invoke() as? T
    }

    @PublishedApi
    internal class LazyInstance<out T : Any>(private val key: DependencyKey) : AbstractLazyProperty<T>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T = graph.provider(key).invoke() as T
    }

    @PublishedApi
    internal class LazyInstanceOrNull<out T : Any?>(private val key: DependencyKey) : AbstractLazyProperty<T?>() {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(graph: Graph): T? = graph.providerOrNull(key)?.invoke() as? T
    }

    inline fun <reified T : Any> instance(qualifier: Any? = null, generics: Boolean = false)
            = register(Instance<T>(typeKey<T>(qualifier, generics)))

    inline fun <reified T : Any?> instanceOrNull(qualifier: Any? = null, generics: Boolean = false)
            = register(InstanceOrNull<T>(typeKey<T>(qualifier, generics)))

    inline fun <reified T : Any> lazyInstance(qualifier: Any? = null, generics: Boolean = false)
            = register(LazyInstance<T>(typeKey<T>(qualifier, generics)))

    inline fun <reified T : Any?> lazyInstanceOrNull(qualifier: Any? = null, generics: Boolean = false)
            = register(LazyInstanceOrNull<T>(typeKey<T>(qualifier, generics)))

    inline fun <reified A, reified R> factory(qualifier: Any? = null, generics: Boolean = false)
            = register(Instance<(A) -> R>(compoundTypeKey<A, R>(qualifier, generics)))

    inline fun <reified A, reified R> factoryOrNull(qualifier: Any? = null, generics: Boolean = false)
            = register(InstanceOrNull<(A) -> R>(compoundTypeKey<A, R>(qualifier, generics)))

    @PublishedApi
    internal fun <T> register(propertyInjector: InjectedProperty<T>): InjectedProperty<T> {
        if (injected) throw IllegalStateException("Injector is already injected.")
        propertyInjectors.add(propertyInjector)
        return propertyInjector
    }

    fun inject(graph: Graph) {
        if (injected) return
        propertyInjectors.forEach { it.inject(graph) }
        propertyInjectors.clear()
        injected = true
    }

}