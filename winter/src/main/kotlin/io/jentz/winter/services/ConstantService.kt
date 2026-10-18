package io.jentz.winter.services

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.Prototype
import io.jentz.winter.Qualifier
import io.jentz.winter.Singleton
import io.jentz.winter.TypeKey

class ConstantService<R : Any?>(
    override val key: TypeKey<R>,
    val value: R
) : BoundService<R>(), UnboundService<R> {

    override val unboundService: UnboundService<R>
        get() = this

    override val requiresPostConstructCallback: Boolean
        get() = false

    override val scope: Qualifier get() = Singleton

    override fun bind(graph: Graph): BoundService<R> = this

    override fun instance(block: ComponentBuilderBlock?): R = value

    override fun newInstance(graph: Graph): R {
        throw AssertionError("BUG: This method should not be called.")
    }

}