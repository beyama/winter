package io.jentz.winter.androidx

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.androidx.inject.PresentationScope
import io.jentz.winter.emptyGraph
import io.jentz.winter.junit4.WinterRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidInjectionAdapterTest {

    private val adapter = AndroidInjectionAdapter(Winter)

    private val winterRule = WinterRule {
        testGraph(ActivityScope::class)
    }

    private val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

    @get:Rule val rule: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() {
                val application = InstrumentationRegistry.getInstrumentation()
                    .targetContext
                    .applicationContext as Application

                Winter.closeGraphIfOpen()

                Winter.component {
                    subcomponent(PresentationScope::class) {
                        subcomponent(ActivityScope::class) {
                        }
                    }
                }
                Winter.injectionAdapter = adapter
                Winter.inject(application)
            }
        })
        .around(winterRule)
        .around(activityScenarioRule)

    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun beforeEach() {
        scenario = activityScenarioRule.scenario
    }

    @Test
    fun use_app_extension_should_register_adapter() {
        val app = WinterApplication().apply { useAndroidInjectionAdapter() }
        assertThat(app.injectionAdapter)
            .isNotNull()
            .isInstanceOf<AndroidInjectionAdapter>()
    }

    @Test
    fun should_get_application_graph_for_application_instance() {
        scenario.onActivity { activity ->
            assertThat(Winter.graph).isSameInstanceAs(adapter.get(activity.application))
        }
    }

    @Test
    fun should_get_activity_graph_for_activity_instance() {
        scenario.onActivity { activity ->
            assertThat(winterRule.requireTestGraph).isSameInstanceAs(adapter.get(activity))
        }
    }

    @Test
    fun should_close_activity_graph_when_activity_gets_destroyed() {
        val graph = winterRule.requireTestGraph

        assertThat(graph.isClosed).isFalse()
        scenario.moveToState(Lifecycle.State.DESTROYED)
        assertThat(graph.isClosed).isTrue()
    }

    @Test
    fun should_retain_presentation_graph_when_activity_gets_recreated() {
        val activityGraph = winterRule.requireTestGraph
        val presentationGraph = activityGraph.parent!!
        activityScenarioRule.scenario.recreate()

        assertThat(presentationGraph.isClosed).isFalse()
    }

    @Test
    fun should_close_presentation_graph_when_activity_gets_destroyed() {
        val activityGraph = winterRule.requireTestGraph
        val presentationGraph = activityGraph.parent!!
        activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)

        assertThat(presentationGraph.isClosed).isTrue()
    }

    fun should_provide_android_types_in_activity_graph() {
        val graph = winterRule.requireTestGraph
        scenario.onActivity { activity ->
            assertThat(graph.instance<Context>())
                .isSameInstanceAs(activity)

            assertThat(graph.instance<Activity>())
                .isSameInstanceAs(activity)

            assertThat(graph.instance<SavedStateRegistryOwner>())
                .isSameInstanceAs(activity)

            assertThat(graph.instance<Lifecycle>())
                .isSameInstanceAs(activity.lifecycle)

            assertThat(graph.instance<SavedStateRegistry>())
                .isSameInstanceAs(activity.savedStateRegistry)

            assertThat(graph.instance<OnBackPressedDispatcherOwner>())
                .isSameInstanceAs(activity)

            assertThat(graph.instance<OnBackPressedDispatcher>())
                .isSameInstanceAs(activity.onBackPressedDispatcher)

            assertThat(graph.instance<ComponentActivity>())
                .isSameInstanceAs(activity)
        }
    }

    @Test
    fun should_get_graph_of_view_context_for_view_instance() {
        scenario.onActivity { activity ->
            val view = View(activity)
            assertThat(winterRule.requireTestGraph).isSameInstanceAs(adapter.get(view))
        }
    }

    @Test
    fun should_get_application_graph_for_broadcast_receiver_instance() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = Unit
        }
        assertThat(Winter.graph).isSameInstanceAs(adapter.get(receiver))
    }

    @Test
    fun should_get_application_graph_for_service_instance() {
        val service = object : Service() {
            override fun onBind(intent: Intent?): IBinder? = null
        }
        assertThat(Winter.graph).isSameInstanceAs(adapter.get(service))
    }

    @Test
    fun should_get_application_graph_for_content_provider_instance() {
        assertThat(Winter.graph).isSameInstanceAs(adapter.get(FileProvider()))
    }

    @Test
    fun should_get_graph_from_dependency_graph_context_wrapper_for_context_wrapper_instance() {
        val graph = emptyGraph()
        scenario.onActivity { activity ->
            assertThat(graph)
                .isSameInstanceAs(adapter.get(WinterContextWrapper(activity, graph)))
        }
    }

}
