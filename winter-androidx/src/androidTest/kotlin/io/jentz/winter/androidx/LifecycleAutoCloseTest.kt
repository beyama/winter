package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_ANY
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.testing.TestLifecycleOwner
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import org.junit.Test

class LifecycleAutoCloseTest {

    @Test
    fun should_throw_if_close_event_is_a_start_event_or_ON_ANY() {
        listOf(ON_CREATE, ON_START, ON_RESUME, ON_ANY).forEach { event ->
            assertFailure {
                LifecycleAutoCloseImpl(event)
            }.isInstanceOf<IllegalArgumentException>()
        }
    }

    @Test
    fun should_call_close_on_close_event_and_unregister_itself() {
        listOf(
            State.RESUMED to ON_PAUSE,
            State.STARTED to ON_STOP,
            State.CREATED to ON_DESTROY
        ).forEach { (initialState, closeEvent) ->
            val lifecycleOwner = TestLifecycleOwner(initialState)
            val observer = LifecycleAutoCloseImpl(closeEvent)
            lifecycleOwner.lifecycle.addObserver(observer)

            lifecycleOwner.handleLifecycleEvent(closeEvent)

            assertThat(observer.closeCalled).isTrue()
            assertThat(lifecycleOwner.observerCount).isEqualTo(0)
        }
    }

    private class LifecycleAutoCloseImpl(
        closeEvent: Lifecycle.Event
    ) : LifecycleAutoClose(closeEvent) {

        var closeCalled = false

        override fun close() {
            closeCalled = true
        }
    }

}