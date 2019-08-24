package io.jentz.winter.plugin

import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Graph
import io.jentz.winter.Scope

/**
 * Empty implementation of [Plugin].
 */
open class SimplePlugin : Plugin {

    override fun graphInitializing(parentGraph: Graph?, builder: ComponentBuilder) {
    }

    override fun postConstruct(graph: Graph, scope: Scope, argument: Any, instance: Any) {
    }

    override fun graphDispose(graph: Graph) {
    }
}
