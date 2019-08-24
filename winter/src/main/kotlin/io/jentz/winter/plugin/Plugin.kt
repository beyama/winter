package io.jentz.winter.plugin

import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Graph
import io.jentz.winter.Scope

/**
 * The interface for Winter plugins.
 */
interface Plugin {
    /**
     * This is called when a [Graph] is initializing and allows to manipulate (derive) the backing
     * [io.jentz.winter.Component].
     *
     * @param parentGraph The parent graph of the new graph that is being initialized.
     * @param builder The [ComponentBuilder] for the new graph.
     */
    fun graphInitializing(parentGraph: Graph?, builder: ComponentBuilder)

    /**
     * This is called whenever a [Graph] is going to be disposed.
     *
     * @param graph The [Graph] that is going to be disposed.
     */
    fun graphDispose(graph: Graph)

    /**
     * This is called whenever a new instance was created.
     *
     * @param graph The [Graph] the instance was created in.
     * @param scope The [Scope] of the instance.
     * @param argument The argument the instance was created with ([Unit] for all non factories).
     * @param instance The instance that was created.
     */
    fun postConstruct(graph: Graph, scope: Scope, argument: Any, instance: Any)

}
