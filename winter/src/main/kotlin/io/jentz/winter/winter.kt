package io.jentz.winter

import io.jentz.winter.internal.DependencyKey

typealias ComponentBuilderBlock = ComponentBuilder.() -> Unit

typealias ProviderBlock<T> = Graph.() -> T
internal typealias UnboundProvider<T> = (Graph) -> T
internal typealias Provider<T> = () -> T

typealias FactoryBlock<A, R> = Graph.(A) -> R
internal typealias UnboundFactory<A, R> = (Graph, A) -> R

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

/**
 * Create an [UnboundProvider] from [ProviderBlock] that evaluates the [block] on invocation and runs the registered
 * [PostConstructPlugin] plugins with the resulting instance.
 *
 * `THIS ISN'T PART OF THE PUBLIC API`
 *
 * @suppress
 */
fun <T : Any> setupProviderBlock(key: DependencyKey, scope: ProviderScope, block: ProviderBlock<T>): UnboundProvider<T> {
    return { graph ->
        val instance = graph.evaluate(key, block)
        WinterPlugins.runPostConstructPlugins(graph, scope, instance)
        instance
    }
}

/**
 * Create an [UnboundFactory] from [FactoryBlock] that evaluates the [block] on invocation.
 *
 * `THIS ISN'T PART OF THE PUBLIC API`
 *
 * @suppress
 */
fun <A : Any, R : Any> setupFactoryBlock(key: DependencyKey, block: FactoryBlock<A, R>): UnboundFactory<A, R> {
    return { graph: Graph, a: A -> graph.evaluate(key, { block(graph, a) }) }
}