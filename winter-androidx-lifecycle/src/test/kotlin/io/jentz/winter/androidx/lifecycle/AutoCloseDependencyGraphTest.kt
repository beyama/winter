package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotlintest.matchers.boolean.shouldBeTrue
import org.junit.Test

class AutoCloseDependencyGraphTest {

    @Test
    fun `should call WinterApplication#closeGraph with WinterAware instance`() {
        val owner = WinterAwareLifecycleOwner()
        val graph = owner.graph
        owner.autoCloseGraphOnDestroy()
        owner.registry.markState(Lifecycle.State.DESTROYED)
        graph.isClosed.shouldBeTrue()
    }

}