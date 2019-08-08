package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleOwner
import io.jentz.winter.Graph

internal class GraphAutoDispose(
    private val graph: Graph,
    disposeEvent: Event
) : LifecycleAutoDispose(disposeEvent) {

    override fun dispose() {
        graph.dispose()
    }
}

/**
 * Automatically dispose this [Graph] when [disposeEvent] is emitted by
 * [androidx.lifecycle.Lifecycle].
 *
 * @param lifecycleOwner The [LifecycleOwner] to observe.
 * @param disposeEvent The [Event] which dispose the graph.
 */
fun Graph.autoDispose(lifecycleOwner: LifecycleOwner, disposeEvent: Event = Event.ON_DESTROY) {
    lifecycleOwner.lifecycle.addObserver(GraphAutoDispose(this, disposeEvent))
}
