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
    val key: TypeKey

    /**
     * A scope that is unique for this type of service e.g. Scope("myCustomScope").
     */
    val scope: Scope

    /**
     * This is called every time an instance is requested from the [Graph].
     *
     * If this service has to create a new instance to satisfy this request it must do the
     * initialization in the block of [Graph.evaluate] and then call [Graph.postConstruct]
     * afterwards.
     *
     *
     * @param argument The argument for this request.
     * @return An instance of type `R`.
     */
    fun instance(argument: A): R

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
    fun postConstruct(arg: Any, instance: Any)

    /**
     * This is called for each [BoundService] in a [Graph] when [Graph.dispose] is called.
     */
    fun dispose()
}

internal class BoundPrototypeService<T : Any>(
    private val graph: Graph,
    private val unboundService: UnboundPrototypeService<T>
) : BoundService<Unit, T> {

    override val scope: Scope get() = Scope.Prototype

    override val key: TypeKey get() = unboundService.key

    override fun instance(argument: Unit): T {
        synchronized(graph) {
            val instance = graph.evaluate(this, argument) { unboundService.factory(graph) }
            graph.postConstruct()
            return instance
        }
    }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, instance as T)
        WinterPlugins.runPostConstructPlugins(graph, scope, Unit, instance)
    }

    override fun dispose() {
    }

}

internal abstract class AbstractBoundSingletonService<T : Any>(
    protected val graph: Graph
) : BoundService<Unit, T> {

    protected abstract val instance: Any

    protected abstract val unboundService: UnboundService<Unit, T>

    final override val key: TypeKey get() = unboundService.key

    final override fun instance(argument: Unit): T {
        val v1 = instance
        if (v1 !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return instance as T
        }

        synchronized(graph) {
            val v2 = instance
            if (instance !== io.jentz.winter.UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return v2 as T
            }

            val typedValue = initialize()
            graph.postConstruct()
            return typedValue
        }
    }

    protected abstract fun initialize(): T

}

internal class BoundSingletonService<T : Any>(
    graph: Graph,
    override val unboundService: UnboundSingletonService<T>
) : AbstractBoundSingletonService<T>(graph) {

    override val scope: Scope get() = Scope.Singleton

    override var instance: Any = UNINITIALIZED_VALUE

    override fun initialize(): T {
        val instance = graph.evaluate(this, Unit) { unboundService.factory(graph) }
        this.instance = instance
        return instance
    }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, instance as T)
        WinterPlugins.runPostConstructPlugins(graph, scope, Unit, instance)
    }

    override fun dispose() {
        val instance = instance
        if (instance !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            unboundService.dispose?.invoke(graph, instance as T)
        }
    }
}

internal class BoundWeakSingletonService<T : Any>(
    graph: Graph,
    override val unboundService: UnboundWeakSingletonService<T>
) : AbstractBoundSingletonService<T>(graph) {

    override val scope: Scope get() = Scope.WeakSingleton

    override val instance: Any get() = reference?.get() ?: UNINITIALIZED_VALUE

    private var reference: WeakReference<T>? = null

    override fun initialize(): T {
        val instance = graph.evaluate(this, Unit) { unboundService.factory(graph) }
        reference = WeakReference(instance)
        return instance
    }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, instance as T)
        WinterPlugins.runPostConstructPlugins(graph, scope, Unit, instance)
    }

    override fun dispose() {
    }

}

internal class BoundSoftSingletonService<T : Any>(
    graph: Graph,
    override val unboundService: UnboundSoftSingletonService<T>
) : AbstractBoundSingletonService<T>(graph) {

    override val instance: Any get() = reference?.get() ?: UNINITIALIZED_VALUE

    override val scope: Scope get() = Scope.SoftSingleton

    private var reference: SoftReference<T>? = null

    override fun initialize(): T {
        val instance = graph.evaluate(this, Unit) { unboundService.factory(graph) }
        reference = SoftReference(instance)
        return instance
    }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, instance as T)
        WinterPlugins.runPostConstructPlugins(graph, scope, Unit, instance)
    }

    override fun dispose() {
    }

}

internal class BoundFactoryService<A, R : Any>(
    private val graph: Graph,
    private val unboundService: UnboundFactoryService<A, R>
) : BoundService<A, R> {

    override val key: TypeKey get() = unboundService.key

    override val scope: Scope get() = Scope.PrototypeFactory

    override fun instance(argument: A): R {
        synchronized(graph) {
            val instance =
                graph.evaluate(this, argument) { unboundService.factory(graph, argument) }
            graph.postConstruct()
            return instance
        }
    }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, arg as A, instance as R)
        WinterPlugins.runPostConstructPlugins(graph, scope, arg, instance)
    }

    override fun dispose() {
    }
}

internal class BoundMultitonFactoryService<A, R : Any>(
    private val graph: Graph,
    private val unboundService: UnboundMultitonFactoryService<A, R>
) : BoundService<A, R> {

    override val key: TypeKey get() = unboundService.key

    override val scope: Scope get() = Scope.MultitonFactory

    private val map = mutableMapOf<A, R>()

    override fun instance(argument: A): R {
        synchronized(graph) {
            map[argument]?.let { return it }

            val instance = graph.evaluate(this, argument) {
                unboundService.factory(graph, argument)
            }
            map[argument] = instance
            graph.postConstruct()
            return instance
        }
    }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, arg as A, instance as R)
        WinterPlugins.runPostConstructPlugins(graph, scope, arg, instance)
    }

    override fun dispose() {
        unboundService.dispose?.let { fn ->
            map.entries.forEach { (argument, instance) -> fn(graph, argument, instance) }
        }
    }
}
