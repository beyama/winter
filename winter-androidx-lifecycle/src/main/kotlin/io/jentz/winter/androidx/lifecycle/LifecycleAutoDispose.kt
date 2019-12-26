package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

/**
 * Abstract LifecycleObserver that calls [dispose] once the [disposeEvent] was emitted.
 *
 * @param disposeEvent The event that will trigger [dispose] must be one of ON_PAUSE, ON_STOP or
 *                     ON_DESTROY.
 */
internal abstract class LifecycleAutoDispose(
    private val disposeEvent: Event
) : LifecycleObserver {

    init {
        require(
            disposeEvent == Event.ON_PAUSE
                    || disposeEvent == Event.ON_STOP
                    || disposeEvent == Event.ON_DESTROY
        ) { "disposeEvent must be ON_PAUSE, ON_STOP or ON_DESTROY" }
    }

    @Suppress("unused")
    @OnLifecycleEvent(Event.ON_ANY)
    fun onEvent(source: LifecycleOwner, event: Event) {
        if (event == disposeEvent) {
            dispose()
            source.lifecycle.removeObserver(this)
        }
    }

    protected abstract fun dispose()

}
