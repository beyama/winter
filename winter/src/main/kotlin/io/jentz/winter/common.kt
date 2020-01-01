package io.jentz.winter

internal val UNINITIALIZED_VALUE = Any()

const val TYPE_KEY_OF_TYPE_QUALIFIER = "__OF_TYPE__"

const val APPLICATION_COMPONENT_QUALIFIER = "application"

/**
 * Factory function signature with [Graph] as receiver.
 */
typealias GFactory<R> = Graph.() -> R

/**
 * Factory callback function signature with [Graph] as receiver.
 * Used for onPostConstruct and onClose callbacks.
 */
typealias GFactoryCallback<R> = Graph.(R) -> Unit

/**
 * Function signature alias for component builder DSL blocks.
 */
typealias ComponentBuilderBlock = Component.Builder.() -> Unit

/**
 * Provider function signature.
 */
typealias Provider<R> = () -> R

/**
 * Members injector signature used in conjunction with JSR330.
 */
typealias MembersInjector<R> = (Graph, R) -> Unit

internal typealias OnCloseCallback = (Graph) -> Unit

/**
 * Key used to store a set of dependency keys of eager dependencies in the dependency map.
 */
internal val eagerDependenciesKey = typeKey<Set<TypeKey<Any>>>("EAGER_DEPENDENCIES")

private val emptyComponent = component {}

/**
 * Returns a [Component] without qualifier and without any declared dependencies.
 */
fun emptyComponent(): Component = emptyComponent

/**
 * Returns a [Graph] with empty component.
 */
fun emptyGraph(): Graph = emptyComponent.createGraph()

/**
 * Create an instance of [Component].
 *
 * @param qualifier A qualifier for the component.
 * @param block A builder block to register provider on the component.
 * @return A instance of component containing all provider defined in the builder block.
 */
fun component(
    qualifier: Any = APPLICATION_COMPONENT_QUALIFIER,
    block: ComponentBuilderBlock
): Component = Component.Builder(qualifier).apply(block).build()

/**
 * Create an ad-hoc instance of [Graph].
 *
 * @param qualifier A qualifier for the backing component.
 * @param block A builder block to register provider on the backing component.
 * @return A instance of component containing all provider defined in the builder block.
 */
fun graph(qualifier: Any = APPLICATION_COMPONENT_QUALIFIER, block: ComponentBuilderBlock): Graph =
    component(qualifier, block).createGraph()

/**
 * Returns [TypeKey] for type [R].
 *
 * @param qualifier An optional qualifier for this key.
 * @param generics If true this creates a type key that also takes generic type parameters into
 *                 account.
 */
inline fun <reified R : Any> typeKey(
    qualifier: Any? = null,
    generics: Boolean = false
): TypeKey<R> = if (generics) {
    object : GenericClassTypeKey<R>(qualifier) {}
} else {
    ClassTypeKey(R::class.java, qualifier)
}

/**
 * This is used internally to created dependency keys to search for all entries of the given type.
 * The qualifier is used to allow the usage of this key for caching to prevent clashes with normal
 * dependency keys.
 */
inline fun <reified R : Any> typeKeyOfType(generics: Boolean = false) =
    typeKey<R>(qualifier = TYPE_KEY_OF_TYPE_QUALIFIER, generics = generics)
