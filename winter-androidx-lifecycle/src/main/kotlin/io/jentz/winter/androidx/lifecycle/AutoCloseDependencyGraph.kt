package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleOwner
import io.jentz.winter.WinterApplication
import io.jentz.winter.aware.WinterAware

internal class AutoCloseDependencyGraph(
    private val application: WinterApplication,
    private val target: Any,
    closeEvent: Event
) : LifecycleAutoClose(closeEvent) {

    override fun close() {
        application.closeGraph(target)
    }

}

/**
 * Automatically call [WinterApplication.closeGraph] with [this] when [closeEvent] is emitted by
 * the [androidx.lifecycle.Lifecycle].
 *
 * This will check if [this] implement [LifecycleOwner] and use it if it doesn't implement
 * [LifecycleOwner] it will try to get an instance from [WinterAware.graph].
 *
 * @param closeEvent The [Event] that will close the graph.
 */
fun WinterAware.autoCloseGraph(closeEvent: Event = Event.ON_DESTROY) {
    val owner: LifecycleOwner = this as? LifecycleOwner ?: graph.instance()
    owner.lifecycle.addObserver(AutoCloseDependencyGraph(winterApplication, this, closeEvent))
}

/**
 * Calls [autoCloseGraph] with [Event.ON_DESTROY]
 */
fun WinterAware.autoCloseGraphOnDestroy() {
    autoCloseGraph(Event.ON_DESTROY)
}

/**
 * Calls [autoCloseGraph] with [Event.ON_STOP]
 */
fun WinterAware.autoCloseGraphOnStop() {
    autoCloseGraph(Event.ON_STOP)
}

/**
 * Calls [autoCloseGraph] with [Event.ON_PAUSE]
 */
fun WinterAware.autoCloseGraphOnPause() {
    autoCloseGraph(Event.ON_PAUSE)
}
