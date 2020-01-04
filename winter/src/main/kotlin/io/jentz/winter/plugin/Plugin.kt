package io.jentz.winter.plugin

import io.jentz.winter.Component
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
     * @param builder The [Component.Builder] for the new graph.
     */
    fun graphInitializing(parentGraph: Graph?, builder: Component.Builder)

    /**
     * This is called when a [Graph] is initialized and before eager dependencies are resolved.
     *
     * @param graph The [Graph] instance.
     */
    fun graphInitialized(graph: Graph)

    /**
     * This is called whenever a [Graph] is going to be closed.
     *
     * @param graph The [Graph] that is going to be closed.
     */
    fun graphClose(graph: Graph)

    /**
     * This is called whenever a new instance was created.
     *
     * @param graph The [Graph] the instance was created in.
     * @param scope The [Scope] of the instance.
     * @param instance The instance that was created.
     */
    fun postConstruct(graph: Graph, scope: Scope, instance: Any)

}
