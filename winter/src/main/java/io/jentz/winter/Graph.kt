package io.jentz.winter

import io.jentz.winter.internal.*
import java.util.*
import kotlin.reflect.KClass

typealias AnyProvider = () -> Any?
typealias AnyFactory = (Any?) -> Any?

class Graph internal constructor(private val parent: Graph?, private val component: Component) {

    private val cache = DependencyMap<AnyProvider>(component.dependencyMap.size)
    private val stack = Stack<DependencyId>()

    inline fun <reified T : Any> instance(qualifier: Any? = null, generics: Boolean = false): T
            = provider<T>(qualifier, generics).invoke()

    inline fun <reified T : Any> instanceOrNull(qualifier: Any? = null, generics: Boolean = false): T?
            = providerOrNull<T>(qualifier, generics)?.invoke()

    inline fun <reified T : Any> provider(qualifier: Any? = null, generics: Boolean = false): () -> T
            = providerOrNull(qualifier, generics) ?: throw EntryNotFoundException("Provider for class `${T::class}` and qualifier `$qualifier` does not exist.")

    inline fun <reified T : Any> providerOrNull(qualifier: Any? = null, generics: Boolean = false): (() -> T)? {
        @Suppress("UNCHECKED_CAST")
        return if (generics) {
            providerOrNull(genericProviderId<T>(qualifier))
        } else {
            providerOrNull(T::class, qualifier)
        } as? () -> T
    }

    inline fun <reified A : Any, reified R : Any> factory(qualifier: Any? = null, generics: Boolean = false): (A) -> R
            = factoryOrNull(qualifier, generics) ?: throw EntryNotFoundException("Factory `(${A::class}) -> ${R::class}` does not exist.")

    inline fun <reified A : Any, reified R : Any> factoryOrNull(qualifier: Any? = null, generics: Boolean = false): ((A) -> R)? {
        @Suppress("UNCHECKED_CAST")
        return if (generics) {
            providerOrNull(genericFactoryId<A, R>(qualifier))
        } else {
            providerOrNull(A::class, R::class, qualifier)
        }?.invoke() as? (A) -> R
    }

    fun provider(id: DependencyId) = providerOrNull(id) ?: throw EntryNotFoundException("Provider with ID `$id` does not exist.")

    fun providerOrNull(id: DependencyId): AnyProvider? = retrieve(
            getCached = { cache[id] },
            getEntry = { component.dependencyMap.getEntry(id) },
            getParent = { parent?.providerOrNull(id) })

    fun providerOrNull(kClass: KClass<*>, qualifier: Any? = null): AnyProvider? = retrieve(
            getCached = { cache.get(kClass, qualifier) },
            getEntry = { component.dependencyMap.getEntry(kClass, qualifier) },
            getParent = { parent?.providerOrNull(kClass, qualifier) })

    fun providerOrNull(argClass: KClass<*>, retClass: KClass<*>, qualifier: Any? = null): AnyProvider? = retrieve(
            getCached = { cache.get(argClass, retClass, qualifier) },
            getEntry = { component.dependencyMap.getEntry(argClass, retClass, qualifier) },
            getParent = { parent?.providerOrNull(argClass, retClass, qualifier) })

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
        val subComponent: Component = instance(name)
        return Graph(this, if (block != null) subComponent.derive(block) else subComponent)
    }

}