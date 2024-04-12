package io.jentz.winter.services

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey

internal abstract class BoundOfTypeService<T : Any, R : Any>(
    protected val graph: Graph
) : BoundService<R>() {

    abstract override val unboundService: OfTypeService<T, R>

    protected val keys: Set<TypeKey<T>> by lazy {
        val typeOfKey = unboundService.typeOfKey
        @Suppress("UNCHECKED_CAST")
        graph.keys().filterTo(mutableSetOf()) { it.typeEquals(typeOfKey) } as Set<TypeKey<T>>
    }

    override fun instance(block: ComponentBuilderBlock?): R =
        graph.evaluate(this, null)

}