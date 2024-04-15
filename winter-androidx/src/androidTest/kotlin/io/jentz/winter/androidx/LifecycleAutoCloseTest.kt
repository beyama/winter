package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.espresso.base.MainThread
import androidx.test.platform.app.InstrumentationRegistry
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.Test

class LifecycleAutoCloseTest : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    @Test
    fun should_throw_if_close_event_is_a_start_event_or_ON_ANY() {
        listOf(ON_CREATE, ON_START, ON_RESUME, ON_ANY).forEach { event ->
            shouldThrow<IllegalArgumentException> {
                LifecycleAutoCloseImpl(event)
            }
        }
    }

    @Test
    fun should_call_close_if_a_close_event_was_emitted() {
        listOf(ON_PAUSE, ON_STOP, ON_DESTROY).forEach { event ->
            val observer = LifecycleAutoCloseImpl(event)
            observer.closeCalled.shouldBeFalse()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                observer.onStateChanged(this, event)
            }
            observer.closeCalled.shouldBeTrue()
        }
    }

    @Test
    fun should_unregister_itself_if_the_close_event_was_emitted() {
        val observer = LifecycleAutoCloseImpl(ON_STOP)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            registry.addObserver(observer)
            observer.onStateChanged(this, ON_STOP)
            registry.observerCount.shouldBe(0)
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