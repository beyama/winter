package io.jentz.winter

import io.jentz.winter.internal.*
import java.util.*
import kotlin.reflect.KClass

typealias AnyProvider = () -> Any?
typealias AnyFactory = (Any?) -> Any?

class Graph internal constructor(private val parent: Graph?, private val component: Component) {

    private val cache = DependencyMap<AnyProvider>(component.dependencyMap.size)
    private val stack = Stack<DependencyId>()

    inline fun <reified T : Any> get(qualifier: Any? = null, generics: Boolean = false): T {
        val provider: AnyProvider = if (generics) {
            getProvider(genericProviderId<T>(qualifier))
        } else {
            getProvider(T::class, qualifier)
        }
        return provider.invoke() as T
    }

    inline fun <reified T : Any?> getOrNull(qualifier: Any? = null, generics: Boolean = false): T? {
        val provider: AnyProvider = if (generics) {
            getProvider(genericProviderId<T>(qualifier))
        } else {
            getProvider(T::class, qualifier)
        }
        return provider.invoke() as T
    }

    inline fun <reified T> getProvider(qualifier: Any? = null, generics: Boolean = false): () -> T {
        val provider = if (generics) {
            getProvider(genericProviderId<T>(qualifier))
        } else {
            getProvider(T::class, qualifier)
        }
        @Suppress("UNCHECKED_CAST")
        return provider as () -> T
    }

    inline fun <reified A, reified R> getFactory(qualifier: Any? = null, generics: Boolean = false): (A) -> R {
        val provider = if (generics) {
            getProvider(genericFactoryId<A, R>(qualifier))
        } else {
            getProvider(A::class, R::class, qualifier)
        }
        @Suppress("UNCHECKED_CAST")
        return provider.invoke() as (A) -> R
    }

    fun getProvider(kClass: KClass<*>, qualifier: Any? = null): AnyProvider
            = retrieve(kClass, qualifier) ?: throw EntryNotFoundException("Entry for class `$kClass` and qualifier `$qualifier` does not exist.")

    fun getProvider(argClass: KClass<*>, retClass: KClass<*>, qualifier: Any? = null): AnyProvider
            = retrieve(argClass, retClass, qualifier) ?: throw EntryNotFoundException("Factory `($argClass) -> $retClass` does not exist.")

    fun getProvider(id: DependencyId): AnyProvider
            = retrieve(id) ?: throw EntryNotFoundException("Provider with ID `$id` does not exist.")

    private fun retrieve(kClass: KClass<*>, qualifier: Any? = null): AnyProvider? = retrieve(
            getCached = { cache.get(kClass, qualifier) },
            getEntry = { component.dependencyMap.getEntry(kClass, qualifier) },
            getParent = { parent?.retrieve(kClass, qualifier) }
    )

    private fun retrieve(argClass: KClass<*>, retClass: KClass<*>, qualifier: Any? = null): AnyProvider? = retrieve(
            getCached = { cache.get(argClass, retClass, qualifier) },
            getEntry = { component.dependencyMap.getEntry(argClass, retClass, qualifier) },
            getParent = { parent?.retrieve(argClass, retClass, qualifier) }
    )

    private fun retrieve(id: DependencyId): AnyProvider? = retrieve(
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

    fun initSubComponent(name: String, block: (ComponentBuilder.() -> Unit)? = null): Graph {
        val subComponent: Component = get(name)
        return Graph(this, if (block != null) subComponent.derive(block) else subComponent)
    }

}