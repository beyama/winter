package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleOwner
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.verify
import io.jentz.winter.Graph
import io.jentz.winter.emptyGraph
import io.kotlintest.matchers.boolean.shouldBeTrue
import org.junit.Before
import org.junit.Test

class GraphAutoDisposeTest {

    private lateinit var owner: LifecycleOwner
    private lateinit var graph: Graph

    @Before
    fun beforeEach() {
        owner = mockLifecycleOwner()
        graph = emptyGraph()
    }

    @Test
    fun `should dispose Graph when event is emitted`() {
        val observer = GraphAutoDispose(graph, ON_DESTROY)
        observer.onEvent(owner, ON_DESTROY)
        graph.isDisposed.shouldBeTrue()
    }

    @Test
    fun `extension method should register observer`() {
        graph.autoDispose(owner)
        verify(owner.lifecycle).addObserver(isA<GraphAutoDispose>())
    }

}