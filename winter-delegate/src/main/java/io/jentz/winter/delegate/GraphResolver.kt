package io.jentz.winter.delegate

import io.jentz.winter.Component
import io.jentz.winter.Graph

fun interface GraphResolver<T> {
    fun resolve(parent: Graph, instance: T): Graph
}

inline fun <reified T: Any> Component.Builder.graphResolver(
    override: Boolean = false,
    resolver: GraphResolver<T>
) = constant<GraphResolver<*>>(resolver, qualifier = T::class, override = override)

inline fun <reified T: Any> Component.Builder.graphResolver(resolver: GraphResolver<T>) =
    constant<GraphResolver<*>>(resolver, qualifier = T::class)

@Suppress("UNCHECKED_CAST")
fun <T: Any> Graph.graphResolver(instance: T): GraphResolver<T>? =
    instance<GraphResolver<*>?>(qualifier = instance::class) as? GraphResolver<T>

fun <T: Any> Graph.resolveGraph(instance: T): Graph =
    graphResolver(instance)?.resolve(this, instance) ?: this