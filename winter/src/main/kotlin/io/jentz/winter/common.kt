package io.jentz.winter

import kotlin.reflect.KClass

internal val UNINITIALIZED_VALUE = Any()

/**
 * Factory function signature with [Graph] as receiver.
 */
typealias GFactory<R> = Graph.() -> R

/**
 * Callback function signature with [Graph] as receiver.
 */
typealias GCallback<R> = Graph.(R) -> Unit

typealias GDisposableSideEffect<T> = Graph.(T) -> GCallback<T>?

/**
 * Function signature alias for component builder DSL blocks.
 */
typealias ComponentBuilderBlock = Component.Builder.() -> Unit

/**
 * Provider function signature.
 */
typealias Provider<R> = () -> R

internal typealias OnCloseCallback = (Graph) -> Unit

/**
 * Key used to store a set of dependency keys of eager dependencies in the dependency map.
 */
internal val eagerDependenciesKey = typeKey<Set<TypeKey<Any>>?>(qualifier("EAGER_DEPENDENCIES"))

/**
 * Returns a [Component] without qualifier and without any declared dependencies.
 */
fun emptyComponent(): Component = Component.EMPTY

/**
 * Returns a [Graph] with empty component.
 */
fun emptyGraph(): Graph = Component.EMPTY.createGraph()

/**
 * Create an instance of [Component].
 *
 * @param qualifier A qualifier for the component.
 * @param block A builder block to register provider on the component.
 * @return A instance of component containing all provider defined in the builder block.
 */
fun component(
    qualifier: Qualifier = ApplicationScope,
    block: ComponentBuilderBlock
): Component = Component.Builder(qualifier).apply(block).build()

/**
 * Create an ad-hoc instance of [Graph].
 *
 * @param qualifier A qualifier for the backing component.
 * @param block A builder block to register provider on the backing component.
 * @return A instance of component containing all provider defined in the builder block.
 */
fun graph(qualifier: Qualifier = ApplicationScope, block: ComponentBuilderBlock): Graph =
    component(qualifier, block).createGraph()

/**
 * Returns [TypeKey] for type [R].
 *
 * @param qualifier An optional qualifier for this key.
 * @param generics If true this creates a type key that also takes generic type parameters into
 *                 account.
 */
inline fun <reified R : Any?> typeKey(
    qualifier: Qualifier? = null,
    generics: Boolean = false
): TypeKey<R> = if (generics) {
    object : GenericClassTypeKey<R>(null is R, qualifier) {}
} else {
    ClassTypeKey(R::class.java, null is R, qualifier)
}

inline fun <reified T: Any> KClass<T>.typeKey(qualifier: Qualifier? = null) =
    ClassTypeKey(java, false, qualifier)
