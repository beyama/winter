package io.jentz.winter

internal val UNINITIALIZED_VALUE = Any()

const val TYPE_KEY_OF_TYPE_QUALIFIER = "__OF_TYPE__"

const val APPLICATION_COMPONENT_QUALIFIER = "application"

/**
 * No argument factory function signature with [Graph] as receiver.
 */
typealias GFactory0<R> = Graph.() -> R

/**
 * One argument factory function signature with [Graph] as receiver.
 */
typealias GFactory1<A, R> = Graph.(A) -> R

/**
 * One argument factory callback function signature with [Graph] as receiver.
 * Used for post-construct and dispose callbacks.
 */
typealias GFactoryCallback1<R> = Graph.(R) -> Unit

/**
 * Two arguments factory callback function signature with [Graph] as receiver.
 * Used for post-construct and dispose callbacks.
 */
typealias GFactoryCallback2<A, R> = Graph.(A, R) -> Unit

/**
 * Provider function signature.
 */
typealias Provider<R> = () -> R

/**
 * Function signature alias for component builder DSL blocks.
 */
typealias ComponentBuilderBlock = ComponentBuilder.() -> Unit

/**
 * Factory function signature.
 */
typealias Factory<A, R> = (A) -> R

internal typealias OnDisposeCallback = (Graph) -> Unit

/**
 * Key used to store a set of dependency keys of eager dependencies in the dependency map.
 */
internal val eagerDependenciesKey = typeKey<Set<TypeKey<Unit, Any>>>("EAGER_DEPENDENCIES")

private val emptyComponent = Component(APPLICATION_COMPONENT_QUALIFIER, emptyMap(), false)

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
): Component = ComponentBuilder(qualifier).apply(block).build()

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
 * Returns [TypeKey] for [MembersInjector] for type [T].
 *
 * Used in conjunction with JSR-330 annotation processor.
 */
inline fun <reified T> membersInjectorKey(): TypeKey<Unit, MembersInjector<T>> {
    /**
     * We use a compound type key without generics to store and retrieve members injectors because
     * they are cheaper than class type keys with generics. But we retrieve them from a service
     * of type BoundService<Unit, MembersInjector<T>> hence this cast.
     */
    @Suppress("UNCHECKED_CAST")
    return CompoundClassTypeKey(T::class.java, MembersInjector::class.java)
            as TypeKey<Unit, MembersInjector<T>>
}

internal fun membersInjectorKey(clazz: Class<*>): TypeKey<Unit, MembersInjector<Any>> {
    /**
     * This is used internally to retrieve members injectors by Java class.
     * @see membersInjectorKey for more details.
     */
    @Suppress("UNCHECKED_CAST")
    return CompoundClassTypeKey(clazz, MembersInjector::class.java)
            as TypeKey<Unit, MembersInjector<Any>>
}

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
): TypeKey<Unit, R> = if (generics) {
    object : GenericClassTypeKey<R>(qualifier) {}
} else {
    ClassTypeKey(R::class.java, qualifier)
}

/**
 * Returns [TypeKey] for type [A] and [R].
 *
 * @param qualifier An optional qualifier for this key.
 * @param generics If true this creates compound type key that also takes generic type parameters
 *                 into account.
 */
inline fun <reified A, reified R : Any> compoundTypeKey(
    qualifier: Any? = null,
    generics: Boolean = false
): TypeKey<A, R> = if (generics) {
    object : GenericCompoundClassTypeKey<A, R>(qualifier) {}
} else {
    CompoundClassTypeKey(A::class.java, R::class.java, qualifier)
}

/**
 * This is used internally to created dependency keys to search for all entries of the given type.
 * The qualifier is used to allow the usage of this key for caching to prevent clashes with normal
 * dependency keys.
 */
inline fun <reified R : Any> typeKeyOfType(generics: Boolean = false) =
    typeKey<R>(qualifier = TYPE_KEY_OF_TYPE_QUALIFIER, generics = generics)
