package io.jentz.winter.delegate

import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.qualifier
import io.jentz.winter.typeKey

fun interface GraphResolver<T> {
    fun resolve(parent: Graph, instance: T): Graph
}

inline fun <reified T: Any> Component.Builder.graphResolver(resolver: GraphResolver<T>) =
    constant<GraphResolver<*>>(resolver, typeKey(T::class.qualifier()))

@Suppress("UNCHECKED_CAST")
fun <T: Any> Graph.graphResolver(instance: T): GraphResolver<T>? =
    instance<GraphResolver<*>?>(typeKey(instance::class.qualifier())) as? GraphResolver<T>

fun <T: Any> Graph.resolveGraph(instance: T): Graph =
    graphResolver(instance)?.resolve(this, instance) ?: this