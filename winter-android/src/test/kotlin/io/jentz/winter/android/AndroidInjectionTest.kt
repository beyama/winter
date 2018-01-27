package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import com.nhaarman.mockito_kotlin.only
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.jentz.winter.Graph
import io.jentz.winter.Injector
import io.jentz.winter.component
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AndroidInjectionTest {

    @Mock lateinit var application: Application
    @Mock lateinit var activity: Activity
    @Mock lateinit var contextWrapper: ContextWrapper
    @Mock lateinit var view: View

    private val rootGraph = component {
        subcomponent("activity") {}
    }.init()

    private lateinit var adapter: TestAdapter

    @Before
    fun beforeEach() {
        whenever(contextWrapper.baseContext).thenReturn(activity)
        whenever(view.context).thenReturn(contextWrapper)
        adapter = TestAdapter(rootGraph)
        AndroidInjection.adapter = adapter
    }

    @Test
    fun `#getApplicationGraph should get application graph from Adapter#getApplicationGraph`() {
        assertSame(adapter.rootGraph, AndroidInjection.getApplicationGraph(application))
    }

    @Test
    fun `#onActivityCreate should pass activity to Adapter#createActivityGraph`() {
        AndroidInjection.onActivityCreate(activity)
        assertSame(activity, adapter.activity)
    }

    @Test
    fun `#onActivityCreate with injector should pass graph to injector`() {
        val injector = Injector()
        val property = injector.instance<Activity>()
        AndroidInjection.onActivityCreate(activity, injector)
        assertSame(activity, property.value)
    }

    @Test
    fun `#onActivityDestroy should call Adapter#disposeActivityGraph`() {
        val graph = AndroidInjection.onActivityCreate(activity)
        AndroidInjection.onActivityDestroy(activity)
        assertNull(adapter.activity)
        assertTrue(graph.isDisposed)
    }

    @Test
    fun `#getActivityGraph should unwrap context wrapper`() {
        AndroidInjection.onActivityCreate(activity)
        AndroidInjection.getActivityGraph(contextWrapper)
        verify(contextWrapper, times(1)).baseContext
    }

    @Test
    fun `#getActivityGraph called with view should use view context to retrieve dependency graph`() {
        AndroidInjection.onActivityCreate(activity)
        AndroidInjection.getActivityGraph(view)
        verify(view, times(1)).context
    }

    private open class TestAdapter(val rootGraph: Graph) : AndroidInjection.Adapter {
        var activityGraph: Graph? = null
        var activity: Activity? = null

        override fun getApplicationGraph(context: Context): Graph = rootGraph

        override fun getActivityGraph(activity: Activity): Graph {
            assertNotNull("activity == null", this.activity)
            assertSame(this.activity, activity)
            val graph = activityGraph ?: throw AssertionError("activityGraph == null")
            return graph
        }

        override fun createActivityGraph(activity: Activity): Graph {
            assertNull("activity != null", this.activity)
            this.activity = activity
            return rootGraph.initSubcomponent("activity") {
                constant(activity)
            }.also { this.activityGraph = it }
        }

        override fun disposeActivityGraph(activity: Activity) {
            assertNotNull("activity == null", this.activity)
            assertSame(this.activity, activity)
            val graph = activityGraph ?: throw AssertionError("activityGraph == null")
            graph.dispose()
            this.activityGraph = null
            this.activity = null
        }
    }

}