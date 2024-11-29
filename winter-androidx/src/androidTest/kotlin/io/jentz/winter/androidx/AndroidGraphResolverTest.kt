package io.jentz.winter.androidx

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycling
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import io.jentz.winter.Graph
import io.jentz.winter.Winter
import io.jentz.winter.androidx.dsl.activityGraphResolver
import io.jentz.winter.androidx.dsl.application
import io.jentz.winter.delegate.graphResolver
import io.jentz.winter.dsl.requireParent
import io.jentz.winter.dsl.singletonOf
import io.jentz.winter.junit4.WinterRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class AndroidGraphResolverTest {

    private lateinit var testGraph: Graph

    private val winterRule = WinterRule {
        withNewGraph(ActivityScope) { testGraph = this }
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
                    activityGraphResolver<TestActivity>()

                    singletonOf(::ApplicationScopedService)
                    subcomponent(ViewModelScope) {
                        singletonOf(::ViewModelScopedService)
                        subcomponent(ActivityScope) {
                            singletonOf(::ActivityScopedService)
                        }
                    }
                }

                Winter.openGraph {
                    application(application)
                }
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
    fun should_close_activity_graph_when_activity_gets_destroyed() {
        val graph = testGraph

        assertThat(graph.isClosed).isFalse()
        scenario.moveToState(Lifecycle.State.DESTROYED)
        assertThat(graph.isClosed).isTrue()
    }


    @Test
    fun should_retain_view_model_graph() {
        scenario.onActivity { activity ->
            assertThat(activity.graph).isSameInstanceAs(testGraph)
        }
        val viewModelScope = testGraph.requireParent

        scenario.recreate()

        assertThat(viewModelScope.isClosed).isFalse()
    }

}