package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleOwner
import io.jentz.winter.Injection
import io.jentz.winter.WinterInjection
import io.jentz.winter.aware.WinterAware

internal class AutoDisposeDependencyGraph(
    private val lifecycleOwner: LifecycleOwner,
    disposeEvent: Event
) : LifecycleAutoDispose(disposeEvent) {

    private val injection: WinterInjection =
        (lifecycleOwner as? WinterAware)?.winterInjection ?: Injection

    override fun dispose() {
        injection.disposeGraph(lifecycleOwner)
    }

}

/**
 * Automatically call [WinterInjection.disposeGraph] with [this] when [disposeEvent] is emitted by
 * [androidx.lifecycle.Lifecycle].
 *
 * This will use [WinterAware.winterInjection] if the [LifecycleOwner] implements [WinterAware]
 * otherwise it will fallback to the default implementation [Injection].
 *
 * @param disposeEvent The [Event] that will dispose the graph.
 */
fun LifecycleOwner.autoDisposeDependencyGraph(disposeEvent: Event = Event.ON_DESTROY) {
    lifecycle.addObserver(AutoDisposeDependencyGraph(this, disposeEvent))
}
