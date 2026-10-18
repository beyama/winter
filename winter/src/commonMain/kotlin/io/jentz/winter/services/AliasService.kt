package io.jentz.winter.services

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.EntryNotFoundException
import io.jentz.winter.Graph
import io.jentz.winter.Prototype
import io.jentz.winter.Qualifier
import io.jentz.winter.TypeKey
import io.jentz.winter.WinterException

internal class AliasService<R : Any?>(
    private val targetKey: TypeKey<*>,
    private val newKey: TypeKey<R>
) : UnboundService<R> {

    override val key: TypeKey<R> get() = newKey

    override val scope: Qualifier get() = Prototype

    override val requiresPostConstructCallback: Boolean get() = false

    override fun bind(graph: Graph): BoundService<R> {
        try {
            @Suppress("UNCHECKED_CAST")
            val targetService = graph.service(targetKey as TypeKey<R>)
                ?: throw EntryNotFoundException(targetKey)
            return BoundAliasService(this, targetService)
        } catch (t: Throwable) {
            throw WinterException("Error resolving alias `$newKey` pointing to `$targetKey`.", t)
        }
    }

}

private class BoundAliasService<R : Any?>(
    override val unboundService: AliasService<R>,
    private val targetService: BoundService<R>
) : BoundService<R>() {

    override fun instance(block: ComponentBuilderBlock?): R = targetService.instance(block)

    override fun newInstance(graph: Graph): R {
        throw AssertionError("BUG: This method should not be called.")
    }

}