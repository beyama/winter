package io.jentz.winter.services

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.GCallback
import io.jentz.winter.GDisposableSideEffect
import io.jentz.winter.GFactory
import io.jentz.winter.Graph
import io.jentz.winter.Scope
import io.jentz.winter.TypeKey
import io.jentz.winter.UNINITIALIZED_VALUE

class SingletonService<R : Any>(
    override val key: TypeKey<R>,
    internal val factory: GFactory<R>
) : UnboundService<R> {

    internal var sideEffects: List<GDisposableSideEffect<R>> = emptyList()

    override val scope: Scope
        get() = Scope.Singleton

    override val requiresPostConstructCallback: Boolean
        get() = sideEffects.isNotEmpty()

    override fun bind(graph: Graph): BoundService<R> =
        BoundSingletonService(graph, this)

    fun addSideEffect(sideEffect: GDisposableSideEffect<R>): SingletonService<R> {
        sideEffects += sideEffect
        return this
    }

    fun onPostConstruct(callback: GCallback<R>) = addSideEffect {
        callback(it)
        null
    }

    fun onClose(callback: GCallback<R>) = addSideEffect { callback }
}

private class BoundSingletonService<R : Any>(
    private val graph: Graph,
    override val unboundService: SingletonService<R>
) : BoundService<R>() {

    @Volatile private var _value = UNINITIALIZED_VALUE
    private var closeCallbacks: List<GCallback<R>>? = null

    @Suppress("UNCHECKED_CAST")
    override fun instance(block: ComponentBuilderBlock?): R {
        val v1 = _value
        if (v1 !== UNINITIALIZED_VALUE) {
            return v1 as R
        }
        synchronized(this) {
            val v2 = _value
            if (v2 !== UNINITIALIZED_VALUE) {
                return v2 as R
            }
            return graph.evaluate(this, block)
        }
    }

    override fun newInstance(graph: Graph): R =
        unboundService.factory(graph).also { _value = it }

    override fun onPostConstruct(graph: Graph, instance: R) {
        if (unboundService.sideEffects.isEmpty()) return
        closeCallbacks = unboundService.sideEffects.mapNotNull { graph.it(instance) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onClose() {
        val callbacks = closeCallbacks.takeUnless { it.isNullOrEmpty() } ?: return
        val instance = _value.takeUnless { it === UNINITIALIZED_VALUE } ?: return
        callbacks.reversed().forEach { graph.it(instance as R) }
    }

}