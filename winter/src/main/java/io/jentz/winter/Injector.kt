package io.jentz.winter

import io.jentz.winter.internal.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Injector {
    private var propertyInjectors = mutableListOf<PropertyInjector<*>>()

    interface PropertyInjector<out T> : ReadOnlyProperty<Any?, T> {
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
    }

    class Instance<out T>(private val id: DependencyId) : AbstractEagerProperty<T>() {
        override fun getValue(graph: Graph): T {
            val provider = graph.getProvider(id)
            @Suppress("UNCHECKED_CAST")
            return provider() as T
        }
    }

    class CurriedFactory<in A, out R>(private val id: DependencyId, private val argument: A) : AbstractEagerProperty<() -> R>() {
        override fun getValue(graph: Graph): () -> R {
            @Suppress("UNCHECKED_CAST")
            val provider = graph.getProvider(id) as () -> (A) -> R
            val factory = provider()
            return { factory(argument) }
        }
    }

    class LazyInstance<out T>(private val id: DependencyId) : AbstractLazyProperty<T>() {
        override fun getValue(graph: Graph): T {
            val provider = graph.getProvider(id)
            @Suppress("UNCHECKED_CAST")
            return provider() as T
        }
    }

    inline fun <reified T : Any> instance(qualifier: String? = null, generics: Boolean = false)
            = register(Instance<T>(if (generics) genericProviderId<T>(qualifier) else providerId<T>(qualifier)))

    inline fun <reified T : Any?> instanceOrNull(qualifier: String? = null, generics: Boolean = false)
            = register(Instance<T>(if (generics) genericProviderId<T>(qualifier) else providerId<T>(qualifier)))

    inline fun <reified A, reified R> factory(qualifier: String? = null, generics: Boolean = false)
            = register(Instance<(A) -> R>(if (generics) genericFactoryId<A, R>(qualifier) else factoryId<A, R>(qualifier)))

    inline fun <reified A, reified R> curriedFactory(argument: A, qualifier: String? = null, generics: Boolean = false)
            = register(CurriedFactory<A, R>(if (generics) genericFactoryId<A, R>(qualifier) else factoryId<A, R>(qualifier), argument))

    inline fun <reified T : Any> lazyInstance(qualifier: String? = null, generics: Boolean = false)
            = register(LazyInstance<T>(if (generics) genericProviderId<T>(qualifier) else providerId<T>(qualifier)))

    inline fun <reified T : Any?> lazyInstanceOrNull(qualifier: String? = null, generics: Boolean = false)
            = register(LazyInstance<T>(if (generics) genericProviderId<T>(qualifier) else providerId<T>(qualifier)))

    fun <T> register(propertyInjector: PropertyInjector<T>): ReadOnlyProperty<Any, T> {
        propertyInjectors.add(propertyInjector)
        return propertyInjector
    }

    fun inject(graph: Graph) {
        propertyInjectors.forEach { it.inject(graph) }
    }

}