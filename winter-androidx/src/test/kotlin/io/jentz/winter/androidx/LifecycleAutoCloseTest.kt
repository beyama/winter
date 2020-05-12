package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.Test

class LifecycleAutoCloseTest : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = registry

    @Test
    fun `should throw an exception if the close event is a start event or ON_ANY`() {
        listOf(ON_CREATE, ON_START, ON_RESUME, ON_ANY).forEach { event ->
            shouldThrow<IllegalArgumentException> {
                LifecycleAutoCloseImpl(event)
            }
        }
    }

    @Test
    fun `should call close if a close event was emitted`() {
        listOf(ON_PAUSE, ON_STOP, ON_DESTROY).forEach { event ->
            val observer = LifecycleAutoCloseImpl(event)
            observer.closeCalled.shouldBeFalse()
            observer.onStateChanged(this, event)
            observer.closeCalled.shouldBeTrue()
        }
    }

    @Test
    fun `should unregister itself if the close event was emitted`() {
        val observer = LifecycleAutoCloseImpl(ON_STOP)
        registry.addObserver(observer)
        observer.onStateChanged(this, ON_STOP)
        registry.observerCount.shouldBe(0)
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