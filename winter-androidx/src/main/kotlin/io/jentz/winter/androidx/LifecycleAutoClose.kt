package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

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
