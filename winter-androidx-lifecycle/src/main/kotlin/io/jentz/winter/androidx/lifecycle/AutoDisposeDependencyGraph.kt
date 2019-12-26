package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleOwner
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.aware.WinterAware

internal class AutoDisposeDependencyGraph(
    private val lifecycleOwner: LifecycleOwner,
    disposeEvent: Event
) : LifecycleAutoDispose(disposeEvent) {

    private val winterApplication: WinterApplication =
        (lifecycleOwner as? WinterAware)?.winterApplication ?: Winter

    override fun dispose() {
        winterApplication.disposeGraph(lifecycleOwner)
    }

}

/**
 * Automatically call [WinterApplication.disposeGraph] with [this] when [disposeEvent] is emitted by
 * the [androidx.lifecycle.Lifecycle].
 *
 * This will use [WinterAware.winterApplication] if the [LifecycleOwner] implements [WinterAware]
 * otherwise it will fallback to the default implementation [Winter].
 *
 * @param disposeEvent The [Event] that will dispose the graph.
 */
fun LifecycleOwner.autoDisposeGraph(disposeEvent: Event = Event.ON_DESTROY) {
    lifecycle.addObserver(AutoDisposeDependencyGraph(this, disposeEvent))
}
