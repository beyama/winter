package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import com.nhaarman.mockito_kotlin.whenever
import io.jentz.winter.GraphRegistry
import io.jentz.winter.WinterException
import io.jentz.winter.component
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

    private val adapter = AndroidPresentationScopeAdapter()

    private val applicationComponent = component {
        subcomponent("presentation") {
            subcomponent("activity") {
            }
        }
    }

    @Before
    fun beforeEach() {
        if (GraphRegistry.has()) GraphRegistry.close()
        GraphRegistry.component = applicationComponent
    }

    @Test
    fun `#createGraph with an Application instance should open root graph with application as constant`() {
        val graph = adapter.createGraph(application, null)

        graph.component.qualifier.shouldBe(null)
        graph.instance<Application>().shouldBe(application)
        graph.instance<Context>().shouldBe(application)
    }

    @Test
    fun `#createGraph with an Application instance and a builder block should apply that builder block to component init`() {
        val instance = Any()
        val graph = adapter.createGraph(application) {
            constant(instance)
        }

        graph.component.qualifier.shouldBe(null)
        graph.instance<Any>().shouldBeSameInstanceAs(instance)
    }

    @Test
    fun `#createGraph with an Activity instance should open presentation and activity graph with activity as constant`() {
        GraphRegistry.open()
        val graph = adapter.createGraph(activity, null)

        graph.component.qualifier.shouldBe("activity")
        graph.instance<Activity>().shouldBe(activity)
        graph.instance<Context>().shouldBe(activity)

        graph.parent?.component?.qualifier.shouldBe("presentation")
    }

    @Test
    fun `#createGraph with an Activity instance and a builder block should apply that builder block to component init`() {
        GraphRegistry.open()
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
    fun `#getGraph called with application should get graph from GraphRegistry`() {
        val graph = GraphRegistry.open()
        adapter.getGraph(application).shouldBe(graph)
    }

    @Test
    fun `#getGraph called with activity should get graph from GraphRegistry`() {
        val presentationIdentifier = activity.javaClass.name
        GraphRegistry.open()
        GraphRegistry.open("presentation", identifier = presentationIdentifier)

        val graph = GraphRegistry.open(presentationIdentifier, "activity", identifier = activity)
        adapter.getGraph(activity).shouldBe(graph)
    }

    @Test
    fun `#getGraph called with view should get graph from the views context`() {
        val graph = applicationComponent.init()
        val contextWrapper = DependencyGraphContextWrapper(context, graph)
        whenever(view.context).thenReturn(contextWrapper)
        adapter.getGraph(view).shouldBe(graph)
    }

    @Test
    fun `#getGraph called with DependencyGraphContextWrapper should get graph from wrapper `() {
        val graph = GraphRegistry.open()
        val contextWrapper = DependencyGraphContextWrapper(context, graph)
        adapter.getGraph(contextWrapper).shouldBe(graph)
    }

    @Test
    fun `#getGraph called with ContextWrapper should get graph from base context`() {
        val graph = GraphRegistry.open()
        whenever(contextWrapper.baseContext).thenReturn(application)
        adapter.getGraph(contextWrapper).shouldBe(graph)
    }

    @Test
    fun `#disposeGraph with Application instance should close root graph`() {
        GraphRegistry.open()
        adapter.disposeGraph(application)
        GraphRegistry.has().shouldBeFalse()
    }

    @Test
    fun `#disposeGraph with Activity instance should only close Activity graph`() {
        val presentationIdentifier = activity.javaClass.name
        GraphRegistry.open()
        GraphRegistry.open("presentation", identifier = presentationIdentifier)
        GraphRegistry.open(presentationIdentifier, "activity", identifier = activity)

        GraphRegistry.has(presentationIdentifier).shouldBeTrue()
        GraphRegistry.has(presentationIdentifier, activity).shouldBeTrue()
        adapter.disposeGraph(activity)
        GraphRegistry.has(presentationIdentifier).shouldBeTrue()
        GraphRegistry.has(presentationIdentifier, activity).shouldBeFalse()
    }

    @Test
    fun `#disposeGraph with Activity instance should close Activity graph when Activity is finishing`() {
        val presentationIdentifier = activity.javaClass.name
        GraphRegistry.open()
        GraphRegistry.open("presentation", identifier = presentationIdentifier)
        GraphRegistry.open(presentationIdentifier, "activity", identifier = activity)

        whenever(activity.isFinishing).thenReturn(true)

        GraphRegistry.has(presentationIdentifier).shouldBeTrue()
        adapter.disposeGraph(activity)
        GraphRegistry.has(presentationIdentifier).shouldBeFalse()
    }


}