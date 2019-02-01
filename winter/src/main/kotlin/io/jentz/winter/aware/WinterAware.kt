package io.jentz.winter.aware

import io.jentz.winter.Graph
import io.jentz.winter.Injection
import io.jentz.winter.WinterInjection

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
     * Get the [WinterInjection] instance to use.
     */
    val injection: WinterInjection get() = Injection

    /**
     * Get the dependency [Graph] to retrieve dependencies from.
     *
     * The default implementation uses [WinterInjection] to get the [Graph] if [WinterInjection]
     * is not used then override this accordingly.
     */
    val graph: Graph get() = injection.getGraph(this)
}
