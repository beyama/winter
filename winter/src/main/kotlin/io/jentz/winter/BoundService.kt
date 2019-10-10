package io.jentz.winter

import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

/**
 * Interface for bound service entries in a [Graph].
 */
interface BoundService<A, R : Any> {
    /**
     * The [TypeKey] of the type this service is providing.
     */
    val key: TypeKey<A, R>

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
     * @param argument The argument for this request.
     * @return An instance of type `R`.
     */
    fun instance(argument: A): R

    /**
     * This is called when this instance is passed to [Graph.evaluate] to create a new instance.
     *
     * If you want to memorize the value this is the place to do it.
     *
     * @param argument The argument for the new instance.
     * @return The new instance of type `R`.
     */
    fun newInstance(argument: A): R

    /**
     * This is called after a new instance was created but not until the complete dependency request
     * is completed.
     *
     * For example:
     * ```
     * graph {
     *   singleton { Parent(child = instance()) }
     *   singleton { Child() }
     * }
     * ```
     * When Parent is requested, Child has to be created but the [postConstruct] method of the
     * Child service is called after Parent is initialized. This way we can resolve cyclic
     * dependencies in post-construct callbacks.
     *
     */
    fun postConstruct(argument: A, instance: R)

    /**
     * This is called for each [BoundService] in a [Graph] when [Graph.dispose] is called.
     */
    fun dispose()
}

internal class BoundPrototypeService<R : Any>(
    private val graph: Graph,
    private val unboundService: UnboundPrototypeService<R>
) : BoundService<Unit, R> {

    override val scope: Scope get() = Scope.Prototype

    override val key: TypeKey<Unit, R> get() = unboundService.key

    override fun instance(argument: Unit): R {
        return graph.evaluate(this, argument)
    }

    override fun newInstance(argument: Unit): R = unboundService.factory(graph)

    override fun postConstruct(argument: Unit, instance: R) {
        unboundService.postConstruct?.invoke(graph, instance)
    }

    override fun dispose() {
    }

}

internal abstract class AbstractBoundSingletonService<R : Any>(
    protected val graph: Graph
) : BoundService<Unit, R> {

    protected abstract val instance: Any

    protected abstract val unboundService: UnboundService<Unit, R>

    final override val key: TypeKey<Unit, R> get() = unboundService.key

    final override fun instance(argument: Unit): R {
        val instance = this.instance
        if (instance !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return instance as R
        }
        return graph.evaluate(this, Unit)
    }

}

internal class BoundSingletonService<R : Any>(
    graph: Graph,
    override val unboundService: UnboundSingletonService<R>
) : AbstractBoundSingletonService<R>(graph) {

    override val scope: Scope get() = Scope.Singleton

    override var instance: Any = UNINITIALIZED_VALUE

    override fun newInstance(argument: Unit): R =
        unboundService.factory(graph).also { instance = it }

    override fun postConstruct(argument: Unit, instance: R) {
        unboundService.postConstruct?.invoke(graph, instance)
    }

    override fun dispose() {
        val instance = instance
        if (instance !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            unboundService.dispose?.invoke(graph, instance as R)
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

    override fun newInstance(argument: Unit): R =
        unboundService.factory(graph).also { reference = WeakReference(it) }

    override fun postConstruct(argument: Unit, instance: R) {
        unboundService.postConstruct?.invoke(graph, instance)
    }

    override fun dispose() {
    }

}

internal class BoundSoftSingletonService<R : Any>(
    graph: Graph,
    override val unboundService: UnboundSoftSingletonService<R>
) : AbstractBoundSingletonService<R>(graph) {

    override val instance: Any get() = reference?.get() ?: UNINITIALIZED_VALUE

    override val scope: Scope get() = Scope.SoftSingleton

    private var reference: SoftReference<R>? = null

    override fun newInstance(argument: Unit): R =
        unboundService.factory(graph).also { reference = SoftReference(it) }

    override fun postConstruct(argument: Unit, instance: R) {
        unboundService.postConstruct?.invoke(graph, instance)
    }

    override fun dispose() {
    }

}

internal class BoundFactoryService<A, R : Any>(
    private val graph: Graph,
    private val unboundService: UnboundFactoryService<A, R>
) : BoundService<A, R> {

    override val key: TypeKey<A, R> get() = unboundService.key

    override val scope: Scope get() = Scope.PrototypeFactory

    override fun instance(argument: A): R = graph.evaluate(this, argument)

    override fun newInstance(argument: A): R = unboundService.factory(graph, argument)

    override fun postConstruct(argument: A, instance: R) {
        unboundService.postConstruct?.invoke(graph, argument, instance)
    }

    override fun dispose() {
    }
}

internal class BoundMultitonFactoryService<A, R : Any>(
    private val graph: Graph,
    private val unboundService: UnboundMultitonFactoryService<A, R>
) : BoundService<A, R> {

    override val key: TypeKey<A, R> get() = unboundService.key

    override val scope: Scope get() = Scope.MultitonFactory

    private val map = mutableMapOf<A, R>()

    override fun instance(argument: A): R =
        map[argument] ?: graph.evaluate(this, argument)

    override fun newInstance(argument: A): R =
        unboundService.factory(graph, argument).also { map[argument] = it }

    override fun postConstruct(argument: A, instance: R) {
        unboundService.postConstruct?.invoke(graph, argument, instance)
    }

    override fun dispose() {
        unboundService.dispose?.let { fn ->
            map.entries.forEach { (argument, instance) -> fn(graph, argument, instance) }
        }
    }
}

internal class BoundGraphService(
        override val key: TypeKey<Unit, Graph>,
        private val graph: Graph
) : BoundService<Unit, Graph> {

    override val scope: Scope
        get() = Scope.Singleton

    override fun instance(argument: Unit): Graph = graph

    override fun newInstance(argument: Unit): Graph {
        throw IllegalStateException(
            "BUG: New instance for BoundGraphService should never be called."
        )
    }

    override fun postConstruct(argument: Unit, instance: Graph) {
    }

    override fun dispose() {
        graph.dispose()
    }
}
