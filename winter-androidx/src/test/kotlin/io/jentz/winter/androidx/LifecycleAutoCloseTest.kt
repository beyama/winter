package io.jentz.winter.androidx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldThrow
import org.junit.Test

class LifecycleAutoCloseTest {

    private val lifecycleOwner: LifecycleOwner = mock { on(it.lifecycle).doReturn(mock()) }

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
            observer.onEvent(lifecycleOwner, event)
            observer.closeCalled.shouldBeTrue()
        }
    }

    @Test
    fun `should unregister itself if the close event was emitted`() {
        val observer = LifecycleAutoCloseImpl(ON_STOP)
        observer.onEvent(lifecycleOwner, ON_STOP)
        verify(lifecycleOwner.lifecycle, times(1)).removeObserver(observer)
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