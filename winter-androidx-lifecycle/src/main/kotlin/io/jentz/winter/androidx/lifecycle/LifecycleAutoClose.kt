package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

/**
 * Abstract LifecycleObserver that calls [close] once the [closeEvent] was emitted.
 *
 * @param closeEvent The event that will trigger [close] must be one of ON_PAUSE, ON_STOP or
 *                     ON_DESTROY.
 */
internal abstract class LifecycleAutoClose(
    private val closeEvent: Event
) : LifecycleObserver {

    init {
        require(
            closeEvent == Event.ON_PAUSE
                    || closeEvent == Event.ON_STOP
                    || closeEvent == Event.ON_DESTROY
        ) { "closeEvent must be ON_PAUSE, ON_STOP or ON_DESTROY" }
    }

    @Suppress("unused")
    @OnLifecycleEvent(Event.ON_ANY)
    fun onEvent(source: LifecycleOwner, event: Event) {
        if (event == closeEvent) {
            close()
            source.lifecycle.removeObserver(this)
        }
    }

    protected abstract fun close()

}
