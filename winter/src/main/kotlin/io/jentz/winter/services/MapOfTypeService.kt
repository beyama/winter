package io.jentz.winter.services

import io.jentz.winter.Graph
import io.jentz.winter.Qualifier
import io.jentz.winter.TypeKey

@PublishedApi
internal class MapOfTypeService<T : Any>(
    key: TypeKey<Map<Qualifier, T>>,
    typeOfKey: TypeKey<T>,
    val defaultKey: Qualifier
) : OfTypeService<T, Map<Qualifier, T>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Map<Qualifier, T>> =
        BoundMapOfTypeService(graph, this)

}

private class BoundMapOfTypeService<T : Any>(
    graph: Graph,
    override val unboundService: MapOfTypeService<T>
) : BoundOfTypeService<T, Map<Qualifier, T>>(graph) {

    override fun newInstance(graph: Graph): Map<Qualifier, T> =
        keys.associateByTo(HashMap(keys.size), {
            it.qualifier ?: unboundService.defaultKey
        }, {
            graph.instance(it)
        })

}