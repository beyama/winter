package io.jentz.winter.androidx.integration.test

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.View
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.androidx.DependencyGraphContextWrapper
import io.jentz.winter.androidx.SimpleAndroidInjectionAdapter
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.androidx.useSimpleAndroidAdapter
import io.jentz.winter.emptyGraph
import io.jentz.winter.junit4.WinterRule
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SimpleAndroidInjectionAdapterTest {

    private val adapter = SimpleAndroidInjectionAdapter(Winter)

    private val winterRule = WinterRule {
        testGraph(ActivityScope::class)
    }

    private val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

    @get:Rule val rule: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() {
                val application: Application = Winter.graph.instance()
                Winter.closeGraph()

                Winter.component {
                    subcomponent(ActivityScope::class) {
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
        val app = WinterApplication().apply { useSimpleAndroidAdapter() }
        app.injectionAdapter.shouldBeInstanceOf<SimpleAndroidInjectionAdapter>()
    }

    @Test
    fun should_get_application_graph_for_application_instance() {
        scenario.onActivity { activity ->
            Winter.graph.shouldBeSameInstanceAs(adapter.get(activity.application))
        }
    }

    @Test
    fun should_get_activity_graph_for_activity_instance() {
        scenario.onActivity { activity ->
            winterRule.requireTestGraph.shouldBeSameInstanceAs(adapter.get(activity))
        }
    }

    @Test
    fun should_close_activity_graph_when_activity_gets_destroyed() {
        val graph = winterRule.requireTestGraph

        graph.isClosed.shouldBeFalse()
        scenario.moveToState(Lifecycle.State.DESTROYED)
        graph.isClosed.shouldBeTrue()
    }

    @Test
    fun should_provide_activity() {
        val graph = winterRule.requireTestGraph
        scenario.onActivity {
            graph.instance<Activity>().shouldBeSameInstanceAs(it)
        }
    }

    @Test
    fun should_provide_lifecycle_from_activity() {
        val graph = winterRule.requireTestGraph
        scenario.onActivity {
            graph.instance<Lifecycle>().shouldBeSameInstanceAs(it.lifecycle)
        }
    }

    @Test
    fun should_provide_activity_as_context() {
        val graph = winterRule.requireTestGraph
        scenario.onActivity {
            graph.instance<Context>().shouldBeSameInstanceAs(it)
        }
    }

    @Test
    fun should_get_graph_of_view_context_for_view_instance() {
        scenario.onActivity { activity ->
            val view = View(activity)
            winterRule.requireTestGraph.shouldBeSameInstanceAs(adapter.get(view))
        }
    }

    @Test
    fun should_get_application_graph_for_broadcast_receiver_instance() {
        Winter.graph.shouldBeSameInstanceAs(adapter.get(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
            }
        }))
    }

    @Test
    fun should_get_application_graph_for_service_instance() {
        Winter.graph.shouldBeSameInstanceAs(adapter.get(object : Service() {
            override fun onBind(intent: Intent?): IBinder? = null
        }))
    }

    @Test
    fun should_get_application_graph_for_content_provider_instance() {
        Winter.graph.shouldBeSameInstanceAs(adapter.get(FileProvider()))
    }

    @Test
    fun should_get_graph_from_dependency_graph_context_wrapper_for_context_wrapper_instance() {
        val graph = emptyGraph()
        scenario.onActivity { activity ->
            adapter.get(DependencyGraphContextWrapper(activity, graph))
                .shouldBeSameInstanceAs(graph)
        }
    }

}
