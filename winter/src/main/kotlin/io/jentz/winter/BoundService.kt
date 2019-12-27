package io.jentz.winter

import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

/**
 * Interface for bound service entries in a [Graph].
 */
interface BoundService<R : Any> {
    /**
     * The [TypeKey] of the type this service is providing.
     */
    val key: TypeKey<R>

    /**
     * A scope that is unique for this type of service e.g. Scope("myCustomScope").
     */
    val scope: Scope

    /**
     * This is called every time an instance is requested from the [Graph].
     *
     * If this service has to create a new instance to satisfy this request it must do the
     * initialization in [newInstance] by calling [Graph.evaluate].
     *
     *
     * @return An instance of type `R`.
     */
    fun instance(): R

    /**
     * This is called when this instance is passed to [Graph.evaluate] to create a new instance.
     *
     * If you want to memorize the value this is the place to do it.
     *
     * @return The new instance of type `R`.
     */
    fun newInstance(): R

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
     */
    fun onPostConstruct(instance: R)

    /**
     * This is called for each [BoundService] in a [Graph] when [Graph.close] is called.
     */
    fun onClose()
}

internal class BoundPrototypeService<R : Any>(
    private val graph: Graph,
    private val unboundService: UnboundPrototypeService<R>
) : BoundService<R> {

    override val scope: Scope get() = Scope.Prototype

    override val key: TypeKey<R> get() = unboundService.key

    override fun instance(): R {
        return graph.evaluate(this)
    }

    override fun newInstance(): R = unboundService.factory(graph)

    override fun onPostConstruct(instance: R) {
        unboundService.onPostConstruct?.invoke(graph, instance)
    }

    override fun onClose() {
    }

}

internal abstract class AbstractBoundSingletonService<R : Any>(
    protected val graph: Graph
) : BoundService<R> {

    protected abstract val instance: Any

    protected abstract val unboundService: UnboundService<R>

    final override val key: TypeKey<R> get() = unboundService.key

    final override fun instance(): R {
        val instance = this.instance
        if (instance !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return instance as R
        }
        return graph.evaluate(this)
    }

}

internal class BoundSingletonService<R : Any>(
    graph: Graph,
    override val unboundService: UnboundSingletonService<R>
) : AbstractBoundSingletonService<R>(graph) {

    override val scope: Scope get() = Scope.Singleton

    override var instance: Any = UNINITIALIZED_VALUE

    override fun newInstance(): R =
        unboundService.factory(graph).also { instance = it }

    override fun onPostConstruct(instance: R) {
        unboundService.onPostConstruct?.invoke(graph, instance)
    }

    override fun onClose() {
        val instance = instance
        if (instance !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            unboundService.onClose?.invoke(graph, instance as R)
        }
    }

}

internal class BoundWeakSingletonService<R : Any>(
    graph: Graph,
    override val unboundService: UnboundWeakSingletonService<R>
) : AbstractBoundSingletonService<R>(graph) {

    override val scope: Scope get() = Scope.WeakSingleton

    override val instance: Any get() = reference?.get() ?: UNINITIALIZED_VALUE

    private var reference: WeakReference<R>? = null

    override fun newInstance(): R =
        unboundService.factory(graph).also { reference = WeakReference(it) }

    override fun onPostConstruct(instance: R) {
        unboundService.onPostConstruct?.invoke(graph, instance)
    }

    override fun onClose() {
    }

}

internal class BoundSoftSingletonService<R : Any>(
    graph: Graph,
    override val unboundService: UnboundSoftSingletonService<R>
) : AbstractBoundSingletonService<R>(graph) {

    override val instance: Any get() = reference?.get() ?: UNINITIALIZED_VALUE

    override val scope: Scope get() = Scope.SoftSingleton

    private var reference: SoftReference<R>? = null

    override fun newInstance(): R =
        unboundService.factory(graph).also { reference = SoftReference(it) }

    override fun onPostConstruct(instance: R) {
        unboundService.onPostConstruct?.invoke(graph, instance)
    }

    override fun onClose() {
    }

}

internal class BoundGraphService(
    override val key: TypeKey<Graph>,
    private val graph: Graph
) : BoundService<Graph> {

    override val scope: Scope
        get() = Scope.Singleton

    override fun instance(): Graph = graph

    override fun newInstance(): Graph {
        throw IllegalStateException(
            "BUG: New instance for BoundGraphService should never be called."
        )
    }

    override fun onPostConstruct(instance: Graph) {
    }

    override fun onClose() {
        graph.close()
    }
}
