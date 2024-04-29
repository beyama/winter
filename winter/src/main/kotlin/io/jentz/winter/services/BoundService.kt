package io.jentz.winter.services

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.Qualifier
import io.jentz.winter.TypeKey

/**
 * Interface for bound service entries in a [Graph].
 */
abstract class BoundService<R : Any?> {

    protected abstract val unboundService: UnboundService<R>

    /**
     * Internal marker if dependency resolution for this is pending to detect cyclic dependencies.
     */
    @JvmField internal var isPending: Boolean = false

    /**
     * The [TypeKey] of the type this service is providing.
     */
    open val key: TypeKey<R> get() = unboundService.key

    /**
     * A scope that is unique for this type of service e.g. Prototype/Singleton.
     */
    open val scope: Qualifier get() = unboundService.scope

    /**
     * Return true if the bound service requires a call to [BoundService.onPostConstruct]
     * after constructing.
     */
    open val requiresPostConstructCallback: Boolean get() = unboundService.requiresPostConstructCallback

    /**
     * This is called every time an instance is requested from the [Graph].
     * Calls to this might not be synchronized.
     *
     * If this service has to create a new instance to satisfy this request it must do the
     * initialization in [newInstance] by calling [Graph.evaluate].
     *
     * @param block An optional builder block to derive the graph to pass runtime values to the
     * factory. This has to be passed to [Graph.evaluate] and if present will result in a extended
     * graph passed to [newInstance].
     *
     * @return An instance of type `R`.
     */
    abstract fun instance(block: ComponentBuilderBlock? = null): R

    /**
     * This is called when this instance is passed to [Graph.evaluate] to create a new instance.
     *
     * If you want to memorize the value this is the place to do it.
     *
     * @param graph The graph that must be used to call this services factory. This is either the
     * [Graph] this [BoundService] was bound to or an extended version if a builder block was passed
     * to [instance].
     *
     * @return The new instance of type `R`.
     */
    abstract fun newInstance(graph: Graph): R

    /**
     * This is called after a new instance was created but not until the complete dependency request
     * is finished.
     *
     * For example:
     * ```
     * graph {
     *   singleton { Parent(child = instance()) }
     *   singleton { Child() }
     * }
     * ```
     * When Parent is requested, Child has to be created but the [onPostConstruct] method of the
     * Child service is called after Parent is initialized. This way we can resolve cyclic
     * dependencies in post-construct callbacks.
     *
     * @param graph The graph that was passed to [newInstance].
     * @param instance The instance that was returned from [newInstance].
     */
    open fun onPostConstruct(graph: Graph, instance: R) {
    }

    /**
     * This is called for each [BoundService] in a [Graph] when [Graph.close] is called.
     */
    open fun onClose() {
    }

}

