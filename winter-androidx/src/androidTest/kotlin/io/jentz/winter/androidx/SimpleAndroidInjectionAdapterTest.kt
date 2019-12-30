package io.jentz.winter.androidx

import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.View
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.Winter
import io.jentz.winter.emptyGraph
import io.jentz.winter.junit4.WinterRule
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
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
        testGraph("activity")
    }

    private val activityScenarioRule = ActivityScenarioRule<TestActivity>(TestActivity::class.java)

    @get:Rule val rule: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() {
                val application: Application = Winter.get().instance()
                Winter.closeIfOpen()

                Winter.component {
                    subcomponent("activity") {
                        constant("test app")
                    }
                }
                Winter.injectionAdapter = adapter
                Winter.inject(application)
            }
        })
        .around(winterRule)
        .around(activityScenarioRule)

    @Test
    fun should_get_application_graph_for_application_instance() {
        activityScenarioRule.scenario.onActivity { activity ->
            Winter.get().shouldBeSameInstanceAs(adapter.get(activity.application))
        }
    }

    @Test
    fun should_get_activity_graph_for_activity_instance() {
        activityScenarioRule.scenario.onActivity { activity ->
            winterRule.requireTestGraph.shouldBeSameInstanceAs(adapter.get(activity))
        }
    }

    @Test
    fun should_get_activity_graph_for_fragment() {
        val fragment = TestFragment()
        activityScenarioRule.scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction().apply {
                add(fragment, "test")
                commit()
            }
        }
        onIdle()

        winterRule.requireTestGraph.shouldBeSameInstanceAs(adapter.get(fragment))
    }

    @Test
    fun should_close_activity_graph_when_activity_gets_destroyed() {
        val graph = winterRule.requireTestGraph

        graph.isClosed.shouldBeFalse()
        activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        graph.isClosed.shouldBeTrue()
    }

    @Test
    fun should_get_graph_of_view_context_for_view_instance() {
        activityScenarioRule.scenario.onActivity { activity ->
            val view = View(activity)
            winterRule.requireTestGraph.shouldBeSameInstanceAs(adapter.get(view))
        }
    }

    @Test
    fun should_get_application_graph_for_broadcast_receiver_instance() {
        Winter.get().shouldBeSameInstanceAs(adapter.get(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
            }
        }))
    }

    @Test
    fun should_get_application_graph_for_service_instance() {
        Winter.get().shouldBeSameInstanceAs(adapter.get(object : Service() {
            override fun onBind(intent: Intent?): IBinder? = null
        }))
    }

    @Test
    fun should_get_application_graph_for_content_provider_instance() {
        Winter.get().shouldBeSameInstanceAs(adapter.get(FileProvider()))
    }

    @Test
    fun should_get_graph_from_dependency_graph_context_wrapper_for_context_wrapper_instance() {
        val graph = emptyGraph()
        activityScenarioRule.scenario.onActivity { activity ->
            adapter.get(DependencyGraphContextWrapper(activity, graph))
                .shouldBeSameInstanceAs(graph)
        }
    }

}
