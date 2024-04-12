package io.jentz.winter.services

import io.jentz.winter.Graph
import io.jentz.winter.Provider
import io.jentz.winter.TypeKey

@PublishedApi
internal class SetOfProvidersForTypeService<T : Any>(
    key: TypeKey<Set<Provider<T>>>,
    typeOfKey: TypeKey<T>
) : OfTypeService<T, Set<Provider<T>>>(key, typeOfKey) {

    override fun bind(graph: Graph): BoundService<Set<Provider<T>>> =
        BoundSetOfProvidersForTypeService(graph, this)

}

private class BoundSetOfProvidersForTypeService<T : Any>(
    graph: Graph,
    override val unboundService: SetOfProvidersForTypeService<T>
) : BoundOfTypeService<T, Set<Provider<T>>>(graph) {

    override fun newInstance(graph: Graph): Set<Provider<T>> =
        keys.mapTo(LinkedHashSet(keys.size)) { graph.providerByKey(it) }

}