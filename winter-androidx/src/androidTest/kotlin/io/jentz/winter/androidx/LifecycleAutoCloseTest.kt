package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_ANY
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import org.junit.Test

class LifecycleAutoCloseTest : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    @Test
    fun should_throw_if_close_event_is_a_start_event_or_ON_ANY() {
        listOf(ON_CREATE, ON_START, ON_RESUME, ON_ANY).forEach { event ->
            assertFailure {
                LifecycleAutoCloseImpl(event)
            }.isInstanceOf<IllegalArgumentException>()
        }
    }

    @Test
    fun should_call_close_if_a_close_event_was_emitted() = runOnMainSync {
        listOf(ON_PAUSE, ON_STOP, ON_DESTROY).forEach { event ->
            val observer = LifecycleAutoCloseImpl(event)
            observer.onStateChanged(this, event)
            assertThat(observer.closeCalled).isTrue()
        }
    }

    @Test
    fun should_unregister_itself_if_the_close_event_was_emitted() = runOnMainSync {
        val observer = LifecycleAutoCloseImpl(ON_STOP)
        registry.addObserver(observer)
        observer.onStateChanged(this, ON_STOP)
        assertThat(registry.observerCount).isEqualTo(0)
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