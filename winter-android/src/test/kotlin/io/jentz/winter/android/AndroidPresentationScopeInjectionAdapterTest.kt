package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import com.nhaarman.mockitokotlin2.whenever
import io.jentz.winter.APPLICATION_COMPONENT_QUALIFIER
import io.jentz.winter.WinterApplication
import io.jentz.winter.WinterException
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AndroidPresentationScopeInjectionAdapterTest {

    @Mock private lateinit var application: Application
    @Mock private lateinit var context: Context
    @Mock private lateinit var activity: Activity
    @Mock private lateinit var view: View
    @Mock private lateinit var contextWrapper: ContextWrapper

    private val app = WinterApplication {
        subcomponent("presentation") {
            subcomponent("activity") {
            }
        }
    }

    private val tree = app.tree

    private val adapter = AndroidPresentationScopeInjectionAdapter(tree)

    @Before
    fun beforeEach() {
        tree.closeIfOpen()
    }

    @Test
    fun `#open with an Application instance should open root graph with application as constant`() {
        val graph = adapter.open(application, null)!!

        graph.component.qualifier.shouldBe(APPLICATION_COMPONENT_QUALIFIER)
        graph.instance<Application>().shouldBe(application)
        graph.instance<Context>().shouldBe(application)
    }

    @Test
    fun `#open with an Application instance and a builder block should derive the component`() {
        val instance = Any()
        val graph = adapter.open(application) { constant(instance) }!!

        graph.component.qualifier.shouldBe(APPLICATION_COMPONENT_QUALIFIER)
        graph.instance<Any>().shouldBeSameInstanceAs(instance)
    }

    @Test
    fun `#open with an Activity instance should open presentation and activity graph with activity as constant`() {
        tree.open()
        val graph = adapter.open(activity, null)!!

        graph.component.qualifier.shouldBe("activity")
        graph.instance<Activity>().shouldBe(activity)
        graph.instance<Context>().shouldBe(activity)

        graph.parent?.component?.qualifier.shouldBe("presentation")
    }

    @Test
    fun `#open with an Activity instance and a builder block should apply that builder block to component init`() {
        tree.open()
        val instance = Any()
        val graph = adapter.open(activity) { constant(instance) }!!
        graph.instance<Any>().shouldBeSameInstanceAs(instance)
    }

    @Test
    fun `#open should return null when instance type isn't supported`() {
        adapter.open(Any(), null).shouldBeNull()
    }

    @Test
    fun `#get called with application should get root graph from tree`() {
        val graph = tree.open()
        adapter.get(application).shouldBe(graph)
    }

    @Test
    fun `#get called with activity should get activity graph from tree`() {
        val presentationIdentifier = activity.javaClass
        tree.open()
        tree.open("presentation", identifier = presentationIdentifier)

        val graph = tree.open(presentationIdentifier, "activity", identifier = activity)
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
    fun `#close with Activity instance should only close Activity graph when activity is not finishing`() {
        val presentationIdentifier = activity.javaClass
        tree.open()
        tree.open("presentation", identifier = presentationIdentifier)
        tree.open(presentationIdentifier, "activity", identifier = activity)

        tree.isOpen(presentationIdentifier).shouldBeTrue()
        tree.isOpen(presentationIdentifier, activity).shouldBeTrue()
        adapter.close(activity)
        tree.isOpen(presentationIdentifier).shouldBeTrue()
        tree.isOpen(presentationIdentifier, activity).shouldBeFalse()
    }

    @Test
    fun `#close with Activity instance should close presentation graph when Activity is finishing`() {
        val presentationIdentifier = activity.javaClass
        tree.open()
        tree.open("presentation", identifier = presentationIdentifier)
        tree.open(presentationIdentifier, "activity", identifier = activity)

        whenever(activity.isFinishing).thenReturn(true)

        tree.isOpen(presentationIdentifier).shouldBeTrue()
        adapter.close(activity)
        tree.isOpen(presentationIdentifier).shouldBeFalse()
    }

}
