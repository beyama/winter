package io.jentz.winter

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InjectionTest {

    private lateinit var adapter: Injection.Adapter

    private val instance = Any()

    private val rootGraph = graph {}

    @BeforeEach
    fun beforeEach() {
        adapter = mock()
        Injection.adapter = adapter
    }

    @Test
    fun `#getGraph should pass instance to Adapter#getGraph and return the result`() {
        whenever(adapter.getGraph(instance)).thenReturn(rootGraph)
        Injection.getGraph(instance).shouldBeSameInstanceAs(rootGraph)
    }

    @Test
    fun `#createGraph should pass instance to Adapter#createGraph and return the result`() {
        whenever(adapter.createGraph(instance, null)).thenReturn(rootGraph)
        Injection.createGraph(instance).shouldBeSameInstanceAs(rootGraph)
    }

    @Test
    fun `#createGraph should pass builder block to Adapter#createGraph`() {
        val block: ComponentBuilderBlock = {}
        whenever(adapter.createGraph(instance, block)).thenReturn(rootGraph)
        Injection.createGraph(instance, block).shouldBeSameInstanceAs(rootGraph)
    }

    @Test
    fun `#createGraphAndInject with injector argument should inject graph into injector`() {
        val injector = Injector()
        whenever(adapter.createGraph(instance, null)).thenReturn(rootGraph)
        Injection.createGraphAndInject(instance, injector).shouldBeSameInstanceAs(rootGraph)
        injector.injected.shouldBeTrue()
    }

    @Test
    fun `#createGraphAndInject should pass builder block to Adapter#createGraph`() {
        val block: ComponentBuilderBlock = {}
        val injector = Injector()
        whenever(adapter.createGraph(instance, block)).thenReturn(rootGraph)
        Injection.createGraphAndInject(instance, injector, block).shouldBeSameInstanceAs(rootGraph)
    }

    @Test
    fun `#disposeGraph should pass instance to Adapter#disposeGraph`() {
        Injection.disposeGraph(instance)
        verify(adapter, times(1)).disposeGraph(instance)
    }

    @Test
    fun `#inject with Injector should get graph for instance and pass it to Injector#inject`() {
        val injector = Injector()
        whenever(adapter.getGraph(instance)).thenReturn(rootGraph)
        Injection.inject(instance, injector)
        injector.injected.shouldBeTrue()
    }

    @Nested
    @DisplayName("JSR330 methods")
    inner class JSR330MemberInjector {

        @Test
        fun `#inject with injection target should call graph#inject`() {
            val graph = mock<Graph>()
            whenever(adapter.getGraph(instance)).thenReturn(graph)

            Injection.inject(instance, false)
            verify(graph, times(1)).inject(instance, false)

            Injection.inject(instance, true)
            verify(graph, times(1)).inject(instance, true)
        }

        @Test
        fun `#createGraphAndInject with injection target should  create graph and call graph#inject on it`() {
            val graph = mock<Graph>()
            whenever(adapter.createGraph(instance, null)).thenReturn(graph)

            Injection.createGraphAndInject(instance, false)
            verify(graph, times(1)).inject(instance, false)

            Injection.createGraphAndInject(instance, true)
            verify(graph, times(1)).inject(instance, true)
        }
    }

}