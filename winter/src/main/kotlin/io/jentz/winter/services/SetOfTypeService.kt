package io.jentz.winter.services

import io.jentz.winter.Graph
import io.jentz.winter.TypeKey

@PublishedApi
internal class SetOfTypeService<T : Any>(
    key: TypeKey<Set<T>>,
    typeOfKey: TypeKey<T>
) : OfTypeService<T, Set<T>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Set<T>> =
        BoundSetOfTypeService(graph, this)

}

private class BoundSetOfTypeService<T : Any>(
    graph: Graph,
    override val unboundService: SetOfTypeService<T>
) : BoundOfTypeService<T, Set<T>>(graph) {

    override fun newInstance(graph: Graph): Set<T> =
        keys.mapTo(LinkedHashSet(keys.size)) { graph.instanceByKey(it) }

}