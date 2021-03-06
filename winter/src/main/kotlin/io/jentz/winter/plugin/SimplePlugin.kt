package io.jentz.winter.plugin

import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.Scope

/**
 * Empty implementation of [Plugin].
 */
open class SimplePlugin : Plugin {

    override fun graphInitializing(parentGraph: Graph?, builder: Component.Builder) {
    }

    override fun graphInitialized(graph: Graph) {
    }

    override fun graphClose(graph: Graph) {
    }

    override fun postConstruct(graph: Graph, scope: Scope, instance: Any) {
    }

}
