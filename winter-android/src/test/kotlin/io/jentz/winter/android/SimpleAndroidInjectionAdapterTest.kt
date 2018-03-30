package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.jentz.winter.GraphRegistry
import io.jentz.winter.WinterException
import io.jentz.winter.component
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SimpleAndroidInjectionAdapterTest {

    @Mock lateinit var application: Application
    @Mock lateinit var context: Context
    @Mock lateinit var activity: Activity
    @Mock lateinit var view: View
    @Mock lateinit var contextWrapper: ContextWrapper

    private val adapter = SimpleAndroidInjectionAdapter()

    private val applicationComponent = component {
        constant("application")
        subcomponent("activity") {
            constant("activity")
        }
    }

    @Before
    fun beforeEach() {
        if (GraphRegistry.has()) GraphRegistry.close()
        GraphRegistry.applicationComponent = applicationComponent
    }

    @Test
    fun `#getApplicationGraph should return root graph`() {
        val graph = GraphRegistry.open()
        assertSame(graph, adapter.getApplicationGraph(context))
    }

    @Test
    fun `#createGraph with an Application instance should open root graph with application as constant`() {
        val application = mock<Application>()
        val graph = adapter.createGraph(application)
        assertEquals("application", graph.instance<String>())
        assertSame(application, graph.instance<Application>())
    }

    @Test
    fun `#createGraph with an Activity instance should open activity graph with activity as constant`() {
        GraphRegistry.open()
        val activity = mock<Activity>()
        val graph = adapter.createGraph(activity)
        assertEquals("activity", graph.instance<String>())
        assertSame(activity, graph.instance<Activity>())
    }

    @Test(expected = WinterException::class)
    fun `#createGraph should throw an exception when instance type isn't supported`() {
        adapter.createGraph(Any())
    }

    @Test
    fun `#getGraph called with application should get graph from GraphRegistry`() {
        val graph = GraphRegistry.open()
        assertSame(graph, adapter.getGraph(application))
    }

    @Test
    fun `#getGraph called with activity should get graph from GraphRegistry`() {
        GraphRegistry.open()
        val graph = GraphRegistry.open("activity", identifier = activity)
        assertSame(graph, adapter.getGraph(activity))
    }

    @Test
    fun `#getGraph called with view should get graph from the views context`() {
        val graph = applicationComponent.init()
        val contextWrapper = DependencyGraphContextWrapper(context, graph)
        whenever(view.context).thenReturn(contextWrapper)
        assertSame(graph, adapter.getGraph(view))
    }

    @Test
    fun `#getGraph called with DependencyGraphContextWrapper should get graph from wrapper `() {
        val graph = GraphRegistry.open()
        val contextWrapper = DependencyGraphContextWrapper(context, graph)
        assertSame(graph, adapter.getGraph(contextWrapper))
    }

    @Test
    fun `#getGraph called with ContextWrapper should get graph from base context`() {
        val graph = GraphRegistry.open()
        whenever(contextWrapper.baseContext).thenReturn(application)
        assertSame(graph, adapter.getGraph(contextWrapper))
    }


}