package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleOwner
import io.jentz.winter.Graph

internal class GraphAutoClose(
    private val graph: Graph,
    disposeEvent: Event
) : LifecycleAutoClose(disposeEvent) {

    override fun close() {
        graph.close()
    }
}

/**
 * Automatically calls [Graph.close] when [closeEvent] is emitted by
 * [androidx.lifecycle.Lifecycle].
 *
 * @param lifecycleOwner The [LifecycleOwner] to observe.
 * @param closeEvent The [Event] which dispose the graph.
 */
private fun Graph.autoClose(lifecycleOwner: LifecycleOwner, closeEvent: Event = Event.ON_DESTROY) {
    lifecycleOwner.lifecycle.addObserver(GraphAutoClose(this, closeEvent))
}

/**
 * Calls [autoClose] with [Event.ON_DESTROY].
 */
fun Graph.autoCloseOnDestroy(lifecycleOwner: LifecycleOwner) {
    autoClose(lifecycleOwner, Event.ON_DESTROY)
}

/**
 * Calls [autoClose] with [Event.ON_STOP].
 */
fun Graph.autoCloseOnStop(lifecycleOwner: LifecycleOwner) {
    autoClose(lifecycleOwner, Event.ON_STOP)
}

/**
 * Calls [autoClose] with [Event.ON_PAUSE].
 */
fun Graph.autoCloseOnPause(lifecycleOwner: LifecycleOwner) {
    autoClose(lifecycleOwner, Event.ON_PAUSE)
}
