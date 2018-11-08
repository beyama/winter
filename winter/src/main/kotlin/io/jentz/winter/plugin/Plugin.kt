package io.jentz.winter.plugin

import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Graph
import io.jentz.winter.Scope

/**
 * The interface for Winter plugins.
 */
interface Plugin {
    /**
     * This is called whenever a [io.jentz.winter.Component] is initializing.
     *
     * @param parentGraph The parent graph of the new graph that is being created.
     * @param builder The [ComponentBuilder] for the new graph.
     */
    fun initializingComponent(parentGraph: Graph?, builder: ComponentBuilder)

    /**
     * This is called whenever a new instance was created.
     *
     * @param graph The [Graph] the instance was created in.
     * @param scope The [Scope] of the instance.
     * @param argument The argument the instance was created with ([Unit] for all non factories).
     * @param instance The instance that was created.
     */
    fun postConstruct(graph: Graph, scope: Scope, argument: Any, instance: Any)

    /**
     * This is called whenever a [Graph] is going to be disposed.
     *
     * @param graph The [Graph] that is going to be disposed.
     */
    fun graphDispose(graph: Graph)
}
