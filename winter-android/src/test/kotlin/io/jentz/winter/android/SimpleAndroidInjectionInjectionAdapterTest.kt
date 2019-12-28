package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import com.nhaarman.mockitokotlin2.whenever
import io.jentz.winter.APPLICATION_COMPONENT_QUALIFIER
import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SimpleAndroidInjectionInjectionAdapterTest {

    @Mock private lateinit var application: Application
    @Mock private lateinit var context: Context
    @Mock private lateinit var activity: Activity
    @Mock private lateinit var view: View
    @Mock private lateinit var contextWrapper: ContextWrapper

    private val app = WinterApplication {
        subcomponent("activity") {
        }
    }

    private val tree = app.tree

    private val adapter = SimpleAndroidInjectionInjectionAdapter(tree)

    @Before
    fun beforeEach() {
        tree.closeIfOpen()
    }

    @Test
    fun `#open with an Application instance should open application graph with application as constant`() {
        val graph = adapter.open(application, null)!!

        graph.component.qualifier.shouldBe(APPLICATION_COMPONENT_QUALIFIER)
        graph.instance<Application>().shouldBe(application)
        graph.instance<Context>().shouldBe(application)
    }

    @Test
    fun `#open with an Application instance and a builder block should derive the component`() {
        val instance = Any()
        val graph = adapter.open(application) { constant(instance) }!!
        graph.instance<Any>().shouldBeSameInstanceAs(instance)
    }

    @Test
    fun `#open with an Activity instance should open activity graph with activity as constant`() {
        tree.open()
        val graph = adapter.open(activity, null)!!

        graph.component.qualifier.shouldBe("activity")
        graph.instance<Activity>().shouldBe(activity)
        graph.instance<Context>().shouldBe(activity)
    }

    @Test
    fun `#open should return null if instance type isn't supported`() {
        adapter.open(Any(), null).shouldBeNull()
    }

    @Test
    fun `#get called with application should get root graph from tree`() {
        val graph = tree.open()
        adapter.get(application).shouldBe(graph)
    }

    @Test
    fun `#get called with activity should get activity graph from tree`() {
        tree.open()
        val graph = tree.open("activity", identifier = activity)
        adapter.get(activity).shouldBe(graph)
    }

    @Test
    fun `#get called with view should get graph from the views context`() {
        val graph = tree.create()
        val contextWrapper = DependencyGraphContextWrapper(context, graph)
        whenever(view.context).thenReturn(contextWrapper)
        adapter.get(view).shouldBe(graph)
    }

    @Test
    fun `#get called with DependencyGraphContextWrapper should get graph from wrapper `() {
        val graph = tree.open()
        val contextWrapper = DependencyGraphContextWrapper(context, graph)
        adapter.get(contextWrapper).shouldBe(graph)
    }

    @Test
    fun `#get called with ContextWrapper should get graph from base context`() {
        val graph = tree.open()
        whenever(contextWrapper.baseContext).thenReturn(application)
        adapter.get(contextWrapper).shouldBe(graph)
    }

    @Test
    fun `#close with Application instance should close root graph`() {
        tree.open()
        adapter.close(application)
        tree.isOpen().shouldBeFalse()
    }

    @Test
    fun `#close with Activity instance should close Activity graph`() {
        tree.open()
        tree.open("activity", identifier = activity)
        adapter.close(activity)
        tree.isOpen(activity).shouldBeFalse()
    }

}