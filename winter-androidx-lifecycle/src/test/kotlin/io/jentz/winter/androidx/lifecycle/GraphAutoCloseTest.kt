package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle
import io.jentz.winter.emptyGraph
import io.kotlintest.matchers.boolean.shouldBeTrue
import org.junit.Test

class GraphAutoCloseTest {

    @Test
    fun `should close Graph when event is emitted`() {
        val owner = WinterAwareLifecycleOwner()
        val graph = emptyGraph()
        graph.autoCloseOnDestroy(owner)
        owner.registry.markState(Lifecycle.State.DESTROYED)
        graph.isClosed.shouldBeTrue()
    }

}