package io.jentz.winter.androidx.lifecyle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import io.jentz.winter.androidx.lifecycle.LifecycleAutoDispose
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test

class LifecycleAutoDisposeTest {

    @Test
    fun `should throw an exception if the dispose event is a start event or ON_ANY`() {
        listOf(ON_CREATE, ON_START, ON_RESUME, ON_ANY).forEach { event ->
            shouldThrow<IllegalArgumentException> {
                LifecycleAutoDisposeImpl(event)
            }
        }
    }

    @Test
    fun `should call dispose if a stop event was emitted`() {
        listOf(ON_PAUSE, ON_STOP, ON_DESTROY).forEach { event ->
            val observer = LifecycleAutoDisposeImpl(event)
            observer.disposeCalled.shouldBeFalse()
            val owner: LifecycleOwner = mockLifecycleOwner()
            observer.onEvent(owner, event)
            observer.disposeCalled.shouldBeTrue()
        }
    }

    @Test
    fun `should unregister itself if the dispose event was emitted`() {
        val observer = LifecycleAutoDisposeImpl(ON_STOP)
        val owner: LifecycleOwner = mockLifecycleOwner()
        observer.onEvent(owner, ON_STOP)
        verify(owner.lifecycle, times(1)).removeObserver(observer)
    }

    private class LifecycleAutoDisposeImpl(
        disposeEvent: Lifecycle.Event
    ) : LifecycleAutoDispose(disposeEvent) {

        var disposeCalled = false

        override fun dispose() {
            disposeCalled = true
        }
    }

}