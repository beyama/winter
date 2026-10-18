package io.jentz.winter

import io.jentz.winter.plugin.Plugin

data class PostConstruct(val graph: Graph, val scope: Qualifier, val instance: Any)

/**
 * Records all plugin calls.
 */
class RecordingPlugin : Plugin {

    /** The parent graphs of [graphInitializing] calls. */
    val graphInitializing = mutableListOf<Graph?>()
    val graphInitialized = mutableListOf<Graph>()
    val graphClose = mutableListOf<Graph>()
    val postConstruct = mutableListOf<PostConstruct>()

    override fun graphInitializing(parentGraph: Graph?, builder: Component.Builder) {
        graphInitializing += parentGraph
    }

    override fun graphInitialized(graph: Graph) {
        graphInitialized += graph
    }

    override fun graphClose(graph: Graph) {
        graphClose += graph
    }

    override fun postConstruct(graph: Graph, scope: Qualifier, instance: Any) {
        postConstruct += PostConstruct(graph, scope, instance)
    }

    fun reset() {
        graphInitializing.clear()
        graphInitialized.clear()
        graphClose.clear()
        postConstruct.clear()
    }

}
