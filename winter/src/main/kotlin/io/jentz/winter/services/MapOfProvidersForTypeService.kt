package io.jentz.winter.services

import io.jentz.winter.Graph
import io.jentz.winter.Provider
import io.jentz.winter.TypeKey

@PublishedApi
internal class MapOfProvidersForTypeService<T : Any>(
    key: TypeKey<Map<Any, Provider<T>>>,
    typeOfKey: TypeKey<T>,
    val defaultKey: Any
) : OfTypeService<T, Map<Any, Provider<T>>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Map<Any, Provider<T>>> =
        BoundMapOfProvidersForTypeService(graph, this)

}

private class BoundMapOfProvidersForTypeService<T : Any>(
    graph: Graph,
    override val unboundService: MapOfProvidersForTypeService<T>
) : BoundOfTypeService<T, Map<Any, Provider<T>>>(graph) {

    override fun newInstance(graph: Graph): Map<Any, Provider<T>> =
        keys.associateByTo(HashMap(keys.size), {
            it.qualifier ?: unboundService.defaultKey
        }, {
            graph.providerByKey(it)
        })

}