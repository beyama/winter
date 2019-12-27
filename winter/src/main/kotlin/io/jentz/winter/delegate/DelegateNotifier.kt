package io.jentz.winter.delegate

import io.jentz.winter.Graph
import java.util.*

/**
 * Holds a map of instances to a list of [injected properties][InjectedProperty] until
 * [Graph.inject] with the instance is called which will trigger [notify].
 *
 * Inspired by Toothpick KTP.
 */
internal class DelegateNotifier {

    private val delegates =
        Collections.synchronizedMap(WeakHashMap<Any, MutableList<InjectedProperty<*>>>())

    fun register(owner: Any, property: InjectedProperty<*>) {
        delegates
            .getOrPut(owner) { mutableListOf() }
            .add(property)
    }

    fun notify(owner: Any, graph: Graph): Boolean {
        return delegates.remove(owner)?.onEach { it.inject(graph) } != null
    }

}
