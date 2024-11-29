package io.jentz.winter.services

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.GCallback
import io.jentz.winter.GFactory
import io.jentz.winter.Graph
import io.jentz.winter.Prototype
import io.jentz.winter.Qualifier
import io.jentz.winter.TypeKey

class PrototypeService<R : Any?>(
    override val key: TypeKey<R>,
    internal val factory: GFactory<R>
) : UnboundService<R> {

    internal var postConstructCallbacks: List<GCallback<R>> = emptyList()

    override val scope: Qualifier
        get() = Prototype

    override val requiresPostConstructCallback: Boolean
        get() = postConstructCallbacks.isNotEmpty()

    override fun bind(graph: Graph): BoundService<R> =
        BoundPrototypeService(graph, this)

    fun onPostConstruct(callback: GCallback<R>): PrototypeService<R> {
        postConstructCallbacks += callback
        return this
    }
}

private class BoundPrototypeService<R : Any?>(
    private val graph: Graph,
    override val unboundService: PrototypeService<R>
) : BoundService<R>() {

    override fun instance(block: ComponentBuilderBlock?): R =
        graph.evaluate(this, block)

    override fun newInstance(graph: Graph): R =
        unboundService.factory(graph)

    override fun onPostConstruct(graph: Graph, instance: R) {
        unboundService.postConstructCallbacks.forEach { graph.it(instance) }
    }

}