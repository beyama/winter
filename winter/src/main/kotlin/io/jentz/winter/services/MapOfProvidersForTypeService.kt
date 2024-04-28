package io.jentz.winter.services

import io.jentz.winter.Graph
import io.jentz.winter.Provider
import io.jentz.winter.Qualifier
import io.jentz.winter.TypeKey

@PublishedApi
internal class MapOfProvidersForTypeService<T : Any>(
    key: TypeKey<Map<Qualifier, Provider<T>>>,
    typeOfKey: TypeKey<T>,
    val defaultKey: Qualifier
) : OfTypeService<T, Map<Qualifier, Provider<T>>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Map<Qualifier, Provider<T>>> =
        BoundMapOfProvidersForTypeService(graph, this)

}

private class BoundMapOfProvidersForTypeService<T : Any>(
    graph: Graph,
    override val unboundService: MapOfProvidersForTypeService<T>
) : BoundOfTypeService<T, Map<Qualifier, Provider<T>>>(graph) {

    override fun newInstance(graph: Graph): Map<Qualifier, Provider<T>> =
        keys.associateByTo(HashMap(keys.size), {
            it.qualifier ?: unboundService.defaultKey
        }, {
            graph.provider(it)
        })

}