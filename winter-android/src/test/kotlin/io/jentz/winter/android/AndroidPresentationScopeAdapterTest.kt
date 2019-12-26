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
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AndroidPresentationScopeAdapterTest {

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

    private val adapter = AndroidPresentationScopeAdapter(tree)

    @Before
    fun beforeEach() {
        tree.closeIfOpen()
    }

    @Test
    fun `#createGraph with an Application instance should open root graph with application and tree as constant`() {
        val graph = adapter.createGraph(application, null)

        graph.component.qualifier.shouldBe(APPLICATION_COMPONENT_QUALIFIER)
        graph.instance<Application>().shouldBe(application)
        graph.instance<Context>().shouldBe(application)
    }

    @Test
    fun `#createGraph with an Application instance and a builder block should apply that builder block to component init`() {
        val instance = Any()
        val graph = adapter.createGraph(application) {
            constant(instance)
        }

        graph.component.qualifier.shouldBe(APPLICATION_COMPONENT_QUALIFIER)
        graph.instance<Any>().shouldBeSameInstanceAs(instance)
    }

    @Test
    fun `#createGraph with an Activity instance should open presentation and activity graph with activity as constant`() {
        tree.open()
        val graph = adapter.createGraph(activity, null)

        graph.component.qualifier.shouldBe("activity")
        graph.instance<Activity>().shouldBe(activity)
        graph.instance<Context>().shouldBe(activity)

        graph.parent?.component?.qualifier.shouldBe("presentation")
    }

    @Test
    fun `#createGraph with an Activity instance and a builder block should apply that builder block to component init`() {
        tree.open()
        val instance = Any()
        val graph = adapter.createGraph(activity) {
            constant(instance)
        }
        graph.instance<Any>().shouldBeSameInstanceAs(instance)
    }

    @Test
    fun `#createGraph should throw an exception when instance type isn't supported`() {
        val instance = Any()
        shouldThrow<WinterException> {
            adapter.createGraph(instance, null)
        }.message.shouldBe("Can't create dependency graph for instance <$instance>.")
    }

    @Test
    fun `#getGraph called with application should get graph from tree`() {
        val graph = tree.open()
        adapter.getGraph(application).shouldBe(graph)
    }

    @Test
    fun `#getGraph called with activity should get graph from tree`() {
        val presentationIdentifier = activity.javaClass
        tree.open()
        tree.open("presentation", identifier = presentationIdentifier)

        val graph = tree.open(presentationIdentifier, "activity", identifier = activity)
        adapter.getGraph(activity).shouldBe(graph)
    }

    @Test
    fun `#getGraph called with view should get graph from the views context`() {
        val graph = tree.create()
        val contextWrapper = DependencyGraphContextWrapper(context, graph)
        whenever(view.context).thenReturn(contextWrapper)
        adapter.getGraph(view).shouldBe(graph)
    }

    @Test
    fun `#getGraph called with DependencyGraphContextWrapper should get graph from wrapper `() {
        val graph = tree.open()
        val contextWrapper = DependencyGraphContextWrapper(context, graph)
        adapter.getGraph(contextWrapper).shouldBe(graph)
    }

    @Test
    fun `#getGraph called with ContextWrapper should get graph from base context`() {
        val graph = tree.open()
        whenever(contextWrapper.baseContext).thenReturn(application)
        adapter.getGraph(contextWrapper).shouldBe(graph)
    }

    @Test
    fun `#disposeGraph with Application instance should close root graph`() {
        tree.open()
        adapter.disposeGraph(application)
        tree.has().shouldBeFalse()
    }

    @Test
    fun `#disposeGraph with Activity instance should only close Activity graph`() {
        val presentationIdentifier = activity.javaClass
        tree.open()
        tree.open("presentation", identifier = presentationIdentifier)
        tree.open(presentationIdentifier, "activity", identifier = activity)

        tree.has(presentationIdentifier).shouldBeTrue()
        tree.has(presentationIdentifier, activity).shouldBeTrue()
        adapter.disposeGraph(activity)
        tree.has(presentationIdentifier).shouldBeTrue()
        tree.has(presentationIdentifier, activity).shouldBeFalse()
    }

    @Test
    fun `#disposeGraph with Activity instance should close Activity graph when Activity is finishing`() {
        val presentationIdentifier = activity.javaClass
        tree.open()
        tree.open("presentation", identifier = presentationIdentifier)
        tree.open(presentationIdentifier, "activity", identifier = activity)

        whenever(activity.isFinishing).thenReturn(true)

        tree.has(presentationIdentifier).shouldBeTrue()
        adapter.disposeGraph(activity)
        tree.has(presentationIdentifier).shouldBeFalse()
    }

}
