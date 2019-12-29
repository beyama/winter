package io.jentz.winter.aware

import io.jentz.winter.Graph
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication

/**
 * Implementing this interface gives simpler syntax for dependency retrieval and is particularly
 * useful in cases where constructor injection is not possible.
 *
 * Example usage:
 * ```
 * class MyClass : WinterAware {
 *   private val service: Service = instance()
 * }
 *```
 */
interface WinterAware {

    /**
     * Get the dependency [Graph] to retrieve dependencies from.
     *
     * The default implementation uses the [WinterApplication.InjectionAdapter] to get the [Graph]
     * if [WinterApplication.InjectionAdapter] is not used override this accordingly.
     */
    val graph: Graph get() = Winter.getGraph(this)

}
