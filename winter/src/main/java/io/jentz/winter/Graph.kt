package io.jentz.winter

import io.jentz.winter.internal.*
import java.util.*
import kotlin.reflect.KClass

typealias AnyProvider = () -> Any?
typealias AnyFactory = (Any?) -> Any?

class Graph internal constructor(private val parent: Graph?, private val component: Component) {

    private val cache = DependencyMap<AnyProvider>(component.dependencyMap.size)
    private val stack = Stack<DependencyId>()

    inline fun <reified T> get(qualifier: Any? = null, generics: Boolean = false): T {
        val provider: AnyProvider = if (generics) {
            retrieve(genericProviderId<T>(qualifier))
        } else {
            retrieve(T::class, qualifier)
        } ?: throw EntryNotFoundException("Provider for class `${T::class}` and qualifier `$qualifier` does not exist.")
        return provider.invoke() as T
    }

    inline fun <reified T> getOrNull(qualifier: Any? = null, generics: Boolean = false): T? {
        val provider: AnyProvider = if (generics) {
            retrieve(genericProviderId<T>(qualifier))
        } else {
            retrieve(T::class, qualifier)
        } ?: return null
        return provider.invoke() as T
    }

    inline fun <reified T> provider(qualifier: Any? = null, generics: Boolean = false): () -> T {
        return providerOrNull(qualifier, generics)
                ?: throw EntryNotFoundException("Entry for class `${T::class}` and qualifier `$qualifier` does not exist.")
    }

    inline fun <reified T> providerOrNull(qualifier: Any? = null, generics: Boolean = false): (() -> T)? {
        @Suppress("UNCHECKED_CAST")
        return if (generics) {
            retrieve(genericProviderId<T>(qualifier)) as? () -> T
        } else {
            retrieve(T::class, qualifier) as? () -> T
        }
    }

    inline fun <reified A, reified R> factory(qualifier: Any? = null, generics: Boolean = false): (A) -> R {
        return factoryOrNull(qualifier, generics)
                ?: throw EntryNotFoundException("Factory `(${A::class}) -> ${R::class}` does not exist.")
    }

    inline fun <reified A, reified R> factoryOrNull(qualifier: Any? = null, generics: Boolean = false): ((A) -> R)? {
        @Suppress("UNCHECKED_CAST")
        return if (generics) {
            retrieve(genericFactoryId<A, R>(qualifier))?.invoke() as? (A) -> R
        } else {
            retrieve(A::class, R::class, qualifier)?.invoke() as? (A) -> R
        }
    }

    fun initSubComponent(name: String, block: (ComponentBuilder.() -> Unit)? = null): Graph {
        val subComponent: Component = get(name)
        return Graph(this, if (block != null) subComponent.derive(block) else subComponent)
    }

    @Suppress("UNCHECKED_CAST")
    fun retrieve(kClass: KClass<*>, qualifier: Any? = null): AnyProvider? = retrieve(
            getCached = { cache.get(kClass, qualifier) },
            getEntry = { component.dependencyMap.getEntry(kClass, qualifier) },
            getParent = { parent?.retrieve(kClass, qualifier) }
    )

    @Suppress("UNCHECKED_CAST")
    fun retrieve(argClass: KClass<*>, retClass: KClass<*>, qualifier: Any? = null): AnyProvider? = retrieve(
            getCached = { cache.get(argClass, retClass, qualifier) },
            getEntry = { component.dependencyMap.getEntry(argClass, retClass, qualifier) },
            getParent = { parent?.retrieve(argClass, retClass, qualifier) }
    )

    fun retrieve(id: DependencyId): AnyProvider? = retrieve(
            getCached = { cache[id] },
            getEntry = { component.dependencyMap.getEntry(id) },
            getParent = { parent?.retrieve(id) })

    private inline fun retrieve(getCached: () -> AnyProvider?,
                                getEntry: () -> DependencyMap.Entry<ComponentEntry>?,
                                getParent: () -> AnyProvider?): AnyProvider? = synchronized(this) {
        getCached()?.let { return@synchronized it }

        val entry = getEntry()

        if (entry != null) {
            val value = entry.value

            val provider: AnyProvider = when (value) {
                is Constant<*> -> wrapConstant(value)
                is Provider<*> -> wrapProvider(entry.key, value.bind(this))
                is Factory<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    wrapFactory(entry.key, value.bind(this) as AnyFactory)
                }
            }

            cache[entry.key] = provider

            return@synchronized provider
        }

        return@synchronized getParent()
    }


    private fun wrapConstant(constant: Constant<*>): AnyProvider = { constant.value }

    private fun wrapProvider(id: DependencyId, block: AnyProvider): AnyProvider = {
        synchronized(this) {
            if (stack.contains(id)) {
                throw CyclicDependencyException("Cyclic dependency for ID `$id`.")
            }
            try {
                stack.push(id)
                block()
            } finally {
                stack.pop()
            }
        }
    }

    private fun wrapFactory(id: DependencyId, block: AnyFactory): AnyProvider {
        val wrapped = { arg: Any? ->
            synchronized(this) {
                if (stack.contains(id)) {
                    throw CyclicDependencyException("Cyclic dependency for ID `$id`.")
                }
                try {
                    stack.push(id)
                    block(arg)
                } finally {
                    stack.pop()
                }
            }
        }
        return { wrapped }
    }

}