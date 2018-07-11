package io.jentz.winter

import io.jentz.winter.internal.*

/**
 * Function signature alias for component builder DSL blocks.
 */
typealias ComponentBuilderBlock = ComponentBuilder.() -> Unit

/**
 * Create an instance of [Component].
 *
 * @param qualifier An optional qualifier for the component.
 * @param block A builder block to register provider on the component.
 * @return A instance of component containing all provider defined in the builder block.
 */
fun component(qualifier: Any? = null, block: ComponentBuilderBlock): Component =
        ComponentBuilder(qualifier).apply(block).build()

/**
 * Create an ad-hoc instance of [Graph].
 *
 * @param qualifier An optional qualifier for the graph.
 * @param block A builder block to register provider on the backing component.
 * @return A instance of component containing all provider defined in the builder block.
 */
fun graph(qualifier: Any? = null, block: ComponentBuilderBlock): Graph = component(qualifier, block).init()

/**
 * Provider function with [Graph] as receiver used in [ComponentBuilder] register methods.
 */
typealias ProviderBlock<T> = Graph.() -> T

/**
 * Function signature alias for provider not bound to a dependency graph.
 */
internal typealias UnboundProvider<T> = (Graph) -> T

/**
 * Function signature alias for provider bound to a dependency graph.
 */
internal typealias Provider<T> = () -> T

/**
 * Factory function with [Graph] as receiver used in [ComponentBuilder] factory register methods.
 */
typealias FactoryBlock<A, R> = Graph.(A) -> R

/**
 * Function signature alias for factories not bound to a dependency graph.
 */
internal typealias UnboundFactory<A, R> = (Graph, A) -> R

/**
 * Returns [DependencyKey] for [MembersInjector] of type [T].
 *
 * Used in conjunction with JSR-330 annotation processor.
 */
inline fun <reified T> membersInjectorKey() = compoundTypeKey<MembersInjector<*>, T>()

/**
 * Returns [DependencyKey] for type [T].
 *
 * @param qualifier An optional qualifier for this key.
 * @param generics If true this creates a type key that also takes generic type parameters into account.
 */
inline fun <reified T> typeKey(qualifier: Any? = null, generics: Boolean = false): DependencyKey =
        if (generics) object : GenericTypeKey<T>(qualifier) {} else TypeKey(T::class.java, qualifier)

/**
 * Returns [DependencyKey] for type [T0] and [T1].
 *
 * @param qualifier An optional qualifier for this key.
 * @param generics If true this creates compound type key that also takes generic type parameters into account.
 */
inline fun <reified T0, reified T1> compoundTypeKey(qualifier: Any? = null, generics: Boolean = false): DependencyKey =
        if (generics) object : GenericCompoundTypeKey<T0, T1>(qualifier) {} else CompoundTypeKey(T0::class.java, T1::class.java, qualifier)

/**
 * This is used internally to created dependency keys to search for all entries of the given type.
 * The qualifier is used to allow the usage of this key for caching to prevent clashes with normal dependency keys.
 */
@PublishedApi
internal inline fun <reified T> typeKeyOfType(generics: Boolean) =
        typeKey<T>(qualifier = "__OF_TYPE__", generics = generics)

/**
 * Interface for all dependency key types.
 */
interface DependencyKey {

    val qualifier: Any?

    /**
     * Test if [other] has the same type.
     * Like [equals] without looking onto the [qualifier].
     */
    fun typeEquals(other: DependencyKey): Boolean
}

/**
 * Key used to store a set of dependency keys of eager dependencies in the dependency map.
 */
internal val eagerDependenciesKey = typeKey<Set<*>>("EAGER_DEPENDENCIES")

internal fun initializeGraph(parentGraph: Graph?, component: Component, block: ComponentBuilderBlock?): Graph {
    val baseComponent = if (WinterPlugins.hasInitializingComponentPlugins || block != null) {
        io.jentz.winter.component {
            include(component)
            block?.invoke(this)
            WinterPlugins.runInitializingComponentPlugins(parentGraph, this)
        }
    } else {
        component
    }
    return Graph(parentGraph, baseComponent)
}

internal fun <T : Any> setupProviderBlock(key: DependencyKey, scope: ProviderScope, block: ProviderBlock<T>): UnboundProvider<T> {
    return { graph ->
        val instance = graph.evaluate(key, block)
        WinterPlugins.runPostConstructPlugins(graph, scope, instance)
        instance
    }
}

internal fun <A : Any, R : Any> setupFactoryBlock(key: DependencyKey, block: FactoryBlock<A, R>): UnboundFactory<A, R> {
    return { graph: Graph, a: A -> graph.evaluate(key, { block(graph, a) }) }
}