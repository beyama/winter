package io.jentz.winter.android

import android.app.Application
import android.content.Context
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.jentz.winter.Graph
import io.jentz.winter.Injector
import io.jentz.winter.component
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AndroidInjectionTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var application: Application
    @Mock private lateinit var adapter: AndroidInjection.Adapter

    private val instance = Any()

    private val rootGraph = component {
        subcomponent("activity") {}
    }.init()

    @Before
    fun beforeEach() {
        AndroidInjection.adapter = adapter
    }

    @Test
    fun `#getApplicationGraph should pass context#applicationContext to Adapter#getGraph and return result`() {
        whenever(adapter.getGraph(application)).thenReturn(rootGraph)
        whenever(context.applicationContext).thenReturn(application)
        assertSame(rootGraph, AndroidInjection.getApplicationGraph(context))
    }

    @Test
    fun `#getGraph should pass instance to Adapter#getGraph and return the result`() {
        whenever(adapter.getGraph(instance)).thenReturn(rootGraph)
        assertSame(rootGraph, AndroidInjection.getGraph(instance))
    }

    @Test
    fun `#createGraph should pass instace to Adapteer#createGraph and return the result`() {
        whenever(adapter.createGraph(instance)).thenReturn(rootGraph)
        assertSame(rootGraph, AndroidInjection.createGraph(instance))
    }

    @Test
    fun `#createGraphAndInject with injector argument should inject graph into injector`() {
        val injector = Injector()
        whenever(adapter.createGraph(instance)).thenReturn(rootGraph)
        assertSame(rootGraph, AndroidInjection.createGraphAndInject(instance, injector))
        assertTrue(injector.injected)
    }

    @Test
    fun `#disposeGraph should pass instance to Adapter#disposeGraph`() {
        AndroidInjection.disposeGraph(instance)
        verify(adapter, times(1)).disposeGraph(instance)
    }

    @Test
    fun `#inject with Injector should get graph for instance and pass it to Injector#inject`() {
        val injector = Injector()
        whenever(adapter.getGraph(instance)).thenReturn(rootGraph)
        AndroidInjection.inject(instance, injector)
        assertTrue(injector.injected)
    }

    @Test
    fun `#inject without Injector argument should call inject on graph (for member injector based injection)`() {
        val graph = mock<Graph>()
        whenever(adapter.getGraph(instance)).thenReturn(graph)
        AndroidInjection.inject(instance)
        verify(graph, times(1)).inject(instance, false)
    }

}