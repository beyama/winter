package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.jentz.winter.Graph
import io.jentz.winter.WinterException

/**
 * Abstract LifecycleObserver that calls [close] and unregisters itself from
 * [source][LifecycleOwner] once the [closeEvent] was emitted.
 *
 * @param closeEvent The event that will trigger [close] must be one of ON_PAUSE, ON_STOP or
 *                   ON_DESTROY.
 */
internal abstract class LifecycleAutoClose(
    private val closeEvent: Event
) : LifecycleEventObserver {

    init {
        require(
            closeEvent == Event.ON_PAUSE
                    || closeEvent == Event.ON_STOP
                    || closeEvent == Event.ON_DESTROY
        ) { "closeEvent must be ON_PAUSE, ON_STOP or ON_DESTROY" }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Event) {
        if (event == closeEvent) {
            close()
            source.lifecycle.removeObserver(this)
        }
    }

    protected abstract fun close()

}

fun Graph.closeWithLifecycle(
    lifecycle: Lifecycle,
    closeEvent: Event = when (lifecycle.currentState) {
        Lifecycle.State.INITIALIZED -> Event.ON_DESTROY
        Lifecycle.State.CREATED -> Event.ON_STOP
        Lifecycle.State.STARTED -> Event.ON_PAUSE
        Lifecycle.State.RESUMED, Lifecycle.State.DESTROYED -> {
            throw WinterException("Cannot setup lifecycle auto close after onResume")
        }
    }
): Graph {
    lifecycle.addObserver(object : LifecycleAutoClose(closeEvent) {
        override fun close() = this@closeWithLifecycle.close()
    })
    return this
}
