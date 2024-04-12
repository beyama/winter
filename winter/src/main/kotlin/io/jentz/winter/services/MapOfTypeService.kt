package io.jentz.winter.services

import io.jentz.winter.Graph
import io.jentz.winter.TypeKey

@PublishedApi
internal class MapOfTypeService<T : Any>(
    key: TypeKey<Map<Any, T>>,
    typeOfKey: TypeKey<T>,
    val defaultKey: Any
) : OfTypeService<T, Map<Any, T>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Map<Any, T>> =
        BoundMapOfTypeService(graph, this)

}

private class BoundMapOfTypeService<T : Any>(
    graph: Graph,
    override val unboundService: MapOfTypeService<T>
) : BoundOfTypeService<T, Map<Any, T>>(graph) {

    override fun newInstance(graph: Graph): Map<Any, T> =
        keys.associateByTo(HashMap(keys.size), {
            it.qualifier ?: unboundService.defaultKey
        }, {
            graph.instanceByKey(it)
        })

}