package io.jentz.winter.dsl

import io.jentz.winter.Component
import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.EntryNotFoundException
import io.jentz.winter.Graph
import io.jentz.winter.Provider
import io.jentz.winter.TypeKey
import io.jentz.winter.erased
import io.jentz.winter.services.checkedInstance

/**
 * [Sequence] with all ancestors and this [Graph] starting with this up the parent hierarchy.
 */
val Graph.selfAndAncestors: Sequence<Graph> get() = generateSequence(this) { it.parent }

/**
 * A sequence of all ancestors. This returns a empty [Sequence] if this is a root [Graph].
 */
val Graph.ancestors get() = selfAndAncestors.drop(1)

/**
 * The root [Graph]. This returns itself if [Graph.parent] is null.
 */
val Graph.root: Graph get() = selfAndAncestors.last()

/**
 * Get parent [Graph].
 *
 * @throws IllegalStateException If parent [Graph] in null.
 */
val Graph.requireParent: Graph get() = checkNotNull(parent) {
    "Parent graph must not be null"
}

/**
 * Returns a set of all [keys][TypeKey] registered on the backing [Component] and all the
 * ancestor components.
 *
 * This is used internally and may be useful for debugging and testing.
 */
fun Graph.keys(): Set<TypeKey<*>> {
    val keys = component.keys()
    return parent?.keys()?.let { keys + it } ?: keys
}

/**
 * Creates a provider function that resolves the service of type [R] on every call.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The provider function.
 *
 * @throws EntryNotFoundException
 */
inline fun <reified R : Any?> Graph.provider(
    noinline block: ComponentBuilderBlock? = null
): Provider<R> = provider(erased(), block)

/**
 * Creates a provider function that resolves the service for [key] on every call.
 * @param key The [TypeKey] of the service to resolve.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The provider function.
 *
 * @throws EntryNotFoundException
 */
fun <R : Any?> Graph.provider(
    key: TypeKey<R>,
    block: ComponentBuilderBlock? = null
): Provider<R> {
    val service = checkedService(key)
    return {
        @Suppress("UNCHECKED_CAST")
        service?.checkedInstance(key.isOptional, block) as R
    }
}

/**
 * Creates an instance of [Lazy] that resolves the service for [R] on first access.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The provider function.
 *
 * @throws EntryNotFoundException
 */
inline fun <reified R: Any?> Graph.lazyInstance(
    noinline block: ComponentBuilderBlock? = null,
): Lazy<R> = lazyInstance(erased(), block)

/**
 * Creates an instance of [Lazy] that resolves the service for [key] on first access.
 * @param key The [TypeKey] of the service to resolve.
 * @param block An optional builder block to pass runtime dependencies to the factory.
 * @return The provider function.
 *
 * @throws EntryNotFoundException
 */
fun <R: Any?> Graph.lazyInstance(
    key: TypeKey<R>,
    block: ComponentBuilderBlock? = null,
): Lazy<R> {
    val service = checkedService(key)
    return lazy {
        @Suppress("UNCHECKED_CAST")
        service?.checkedInstance(key.isOptional, block) as R
    }
}