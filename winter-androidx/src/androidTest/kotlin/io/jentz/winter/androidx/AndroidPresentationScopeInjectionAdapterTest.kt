package io.jentz.winter.androidx

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.Winter
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
class AndroidPresentationScopeInjectionAdapterTest {

    private val adapter = AndroidPresentationScopeInjectionAdapter(Winter)

    private val winterRule = WinterRule {
        testGraph("activity")
    }

    private val activityScenarioRule = ActivityScenarioRule<TestActivity>(TestActivity::class.java)

    @get:Rule val rule: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() {
                val application: Application = Winter.graph.instance()
                Winter.closeGraph()

                Winter.component {
                    subcomponent("presentation") {
                        subcomponent("activity") {
                            constant("test app")
                        }
                    }
                }
                Winter.injectionAdapter = adapter
                Winter.inject(application)
            }
        })
        .around(winterRule)
        .around(activityScenarioRule)

    @Test
    fun should_get_activity_graph_for_activity_instance() {
        activityScenarioRule.scenario.onActivity { activity ->
            winterRule.requireTestGraph.shouldBeSameInstanceAs(adapter.get(activity))
        }
    }

    @Test
    fun should_close_activity_graph_when_activity_gets_recreated() {
        val activityGraph = winterRule.requireTestGraph
        activityScenarioRule.scenario.recreate()

        activityGraph.isClosed.shouldBeTrue()
    }

    @Test
    fun should_retain_presentation_graph_when_activity_gets_recreated() {
        val activityGraph = winterRule.requireTestGraph
        val presentationGraph = activityGraph.parent!!
        activityScenarioRule.scenario.recreate()

        presentationGraph.isClosed.shouldBeFalse()
    }

    @Test
    fun should_close_presentation_graph_when_activity_gets_destroyed() {
        val activityGraph = winterRule.requireTestGraph
        val presentationGraph = activityGraph.parent!!
        activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)

        presentationGraph.isClosed.shouldBeTrue()
    }

}
