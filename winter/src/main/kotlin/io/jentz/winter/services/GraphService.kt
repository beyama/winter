package io.jentz.winter.services

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.Scope
import io.jentz.winter.TypeKey

internal class GraphService(
    override val key: TypeKey<Graph>,
    private val graph: Graph
) : BoundService<Graph>(), UnboundService<Graph> {

    override val unboundService: UnboundService<Graph> get() = this

    override val requiresPostConstructCallback: Boolean
        get() = false

    override val scope: Scope
        get() = Scope.Singleton

    override fun bind(graph: Graph): BoundService<Graph> = this

    override fun instance(block: ComponentBuilderBlock?): Graph = graph

    override fun newInstance(graph: Graph): Graph {
        throw AssertionError("BUG: This method should not be called.")
    }

    override fun onClose() {
        graph.close()
    }
}