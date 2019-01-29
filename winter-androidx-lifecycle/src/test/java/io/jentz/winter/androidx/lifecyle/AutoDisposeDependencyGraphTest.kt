package io.jentz.winter.androidx.lifecyle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockito_kotlin.*
import io.jentz.winter.Injection
import io.jentz.winter.WinterInjection
import io.jentz.winter.androidx.lifecycle.AutoDisposeDependencyGraph
import io.jentz.winter.androidx.lifecycle.autoDisposeDependencyGraph
import io.jentz.winter.aware.WinterAware
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutoDisposeDependencyGraphTest {

    @BeforeEach
    fun beforeEach() {
        Injection.adapter = mock()
    }

    @Test
    fun `should dispose owner with Winter Injection from WinterAware implementation`() {
        val owner = WinterAwareLifecycleOwner()
        val observer = AutoDisposeDependencyGraph(owner, ON_DESTROY)
        observer.onEvent(owner, ON_DESTROY)
        verify(owner.winterInjection.adapter, times(1)).disposeGraph(owner)
        verifyZeroInteractions(Injection.adapter)
    }

    @Test
    fun `should dispose owner with default Injection implementation if owner doesn't implement WinterAware`() {
        val owner = mockLifecycleOwner()
        val observer = AutoDisposeDependencyGraph(owner, ON_DESTROY)
        observer.onEvent(owner, ON_DESTROY)
        verify(Injection.adapter, times(1)).disposeGraph(owner)
    }

    @Test
    fun `extension method should register observer`() {
        val owner = mockLifecycleOwner()
        owner.autoDisposeDependencyGraph()
        verify(owner.lifecycle, times(1)).addObserver(isA<AutoDisposeDependencyGraph>())
    }

    private class WinterAwareLifecycleOwner : WinterAware, LifecycleOwner {
        override val winterInjection: WinterInjection = WinterInjection().apply { adapter = mock() }

        private val lifecycle: Lifecycle = mock()

        override fun getLifecycle(): Lifecycle = lifecycle
    }

}