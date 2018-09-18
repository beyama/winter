package io.jentz.winter

import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

interface BoundService<A, R : Any> {
    val key: DependencyKey
    fun instance(arg: A): R
    fun postConstruct(arg: Any, instance: Any)
    fun dispose()
}

internal class BoundPrototypeService<T : Any>(
        private val graph: Graph,
        private val unboundService: UnboundPrototypeService<T>
) : BoundService<Unit, T> {

    override val key: DependencyKey
        get() = unboundService.key

    override fun instance(arg: Unit): T = graph
            .evaluate(this, arg) { unboundService.block(graph) }
            .also { graph.postConstruct() }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, instance as T)
        WinterPlugins.runPostConstructPlugins(graph, Scope.Prototype, instance)
    }

    override fun dispose() {
    }

}

internal abstract class AbstractBoundSingletonService<T : Any>(
        protected val graph: Graph
) : BoundService<Unit, T> {

    protected abstract val instance: Any

    protected abstract val unboundService: UnboundService<Unit, T>

    final override val key: DependencyKey
        get() = unboundService.key

    final override fun instance(arg: Unit): T {
        val v1 = instance
        if (v1 !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return instance as T
        }

        synchronized(this) {
            val v2 = instance
            if (instance !== io.jentz.winter.UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return v2 as T
            }

            synchronized(graph) {
                val typedValue = initialize()
                graph.postConstruct()
                return typedValue
            }
        }
    }

    protected abstract fun initialize(): T

}

internal class BoundSingletonService<T : Any>(
        graph: Graph,
        override val unboundService: UnboundSingletonService<T>
) : AbstractBoundSingletonService<T>(graph) {

    override var instance: Any = UNINITIALIZED_VALUE

    override fun initialize(): T = graph
            .evaluate(this, Unit) { unboundService.block(graph) }
            .also { this.instance = it }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, instance as T)
        WinterPlugins.runPostConstructPlugins(graph, Scope.Singleton, instance)
    }

    override fun dispose() {
        val instance = instance
        if (instance !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            unboundService.dispose?.invoke(instance as T)
        }
    }
}

internal class BoundWeakSingletonService<T : Any>(
        graph: Graph,
        override val unboundService: UnboundWeakSingletonService<T>
) : AbstractBoundSingletonService<T>(graph) {

    private var reference: WeakReference<T>? = null

    override val instance: Any
        get() = reference?.get() ?: UNINITIALIZED_VALUE

    override fun initialize(): T = graph
            .evaluate(this, Unit) { unboundService.block(graph) }
            .also { this.reference = WeakReference(it) }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, instance as T)
        WinterPlugins.runPostConstructPlugins(graph, Scope.WeakSingleton, instance)
    }

    override fun dispose() {
    }

}

internal class BoundSoftSingletonService<T : Any>(
        graph: Graph,
        override val unboundService: UnboundSoftSingletonService<T>
) : AbstractBoundSingletonService<T>(graph) {

    private var reference: SoftReference<T>? = null

    override val instance: Any
        get() = reference?.get() ?: UNINITIALIZED_VALUE

    override fun initialize(): T = graph
            .evaluate(this, Unit) { unboundService.block(graph) }
            .also { this.reference = SoftReference(it) }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, instance as T)
        WinterPlugins.runPostConstructPlugins(graph, Scope.SoftSingleton, instance)
    }

    override fun dispose() {
    }

}

internal class BoundFactoryService<A, R : Any>(
        private val graph: Graph,
        private val unboundService: UnboundFactoryService<A, R>
) : BoundService<A, R> {

    override val key: DependencyKey
        get() = unboundService.key

    override fun instance(arg: A): R {
        return graph.evaluate(this, arg) { unboundService.block(graph, arg) }
    }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, arg as A, instance as R)
    }

    override fun dispose() {
    }
}

internal class BoundMultitonFactoryService<A, R : Any>(
        private val graph: Graph,
        private val unboundService: UnboundMultitonFactoryService<A, R>
) : BoundService<A, R> {

    private val map = mutableMapOf<A, R>()

    override val key: DependencyKey
        get() = unboundService.key

    override fun instance(arg: A): R {
        synchronized(map) {
            map[arg]?.let { return it }

            val instance = graph.evaluate(this, arg) { unboundService.block(graph, arg) }
            map[arg] = instance
            return instance
        }
    }

    override fun postConstruct(arg: Any, instance: Any) {
        @Suppress("UNCHECKED_CAST")
        unboundService.postConstruct?.invoke(graph, arg as A, instance as R)
    }

    override fun dispose() {
        unboundService.dispose?.let { fn ->
            map.forEach { argument, instance -> fn(argument, instance) }
        }
    }
}