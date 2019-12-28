package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.aware.WinterAware
import org.junit.Before
import org.junit.Test

class AutoDisposeDependencyGraphTest {

    @Before
    fun beforeEach() {
        Winter.injectionAdapter = mock()
    }

    @Test
    fun `should dispose owner with WinterApplication from WinterAware implementation`() {
        val owner = WinterAwareLifecycleOwner()
        val observer = AutoDisposeDependencyGraph(owner, ON_DESTROY)
        observer.onEvent(owner, ON_DESTROY)
        verify(owner.winterApplication.injectionAdapter!!, times(1)).close(owner)
    }

    @Test
    fun `should dispose owner with default Injection implementation if owner doesn't implement WinterAware`() {
        val owner = mockLifecycleOwner()
        val observer = AutoDisposeDependencyGraph(owner, ON_DESTROY)
        observer.onEvent(owner, ON_DESTROY)
        verify(Winter.injectionAdapter!!, times(1)).close(owner)
    }

    @Test
    fun `extension method should register observer`() {
        val owner = mockLifecycleOwner()
        owner.autoDisposeGraph()
        verify(owner.lifecycle, times(1)).addObserver(isA<AutoDisposeDependencyGraph>())
    }

    private class WinterAwareLifecycleOwner : WinterAware, LifecycleOwner {
        override val winterApplication = WinterApplication().apply { injectionAdapter = mock() }

        private val lifecycle: Lifecycle = mock()

        override fun getLifecycle(): Lifecycle = lifecycle
    }

}