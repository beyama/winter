package io.jentz.winter.services

import io.jentz.winter.Graph
import io.jentz.winter.Qualifier
import io.jentz.winter.TypeKey

/**
 * Interface for service entries registered in a [io.jentz.winter.Component].
 *
 * Custom implementations can be added to a [io.jentz.winter.Component] by using
 * [io.jentz.winter.Component.Builder.register].
 */
interface UnboundService<R : Any?> {
    /**
     * The [TypeKey] of the type this service is providing.
     */
    val key: TypeKey<R>

    /**
     * A scope that is unique for this type of service e.g. Prototype/Singleton.
     */
    val scope: Qualifier

    /**
     * Return true if the bound service requires a call to [BoundService.onPostConstruct]
     * after constructing.
     */
    val requiresPostConstructCallback: Boolean

    /**
     * Returns a [BoundService] for this [UnboundService].
     */
    fun bind(graph: Graph): BoundService<R>

}

