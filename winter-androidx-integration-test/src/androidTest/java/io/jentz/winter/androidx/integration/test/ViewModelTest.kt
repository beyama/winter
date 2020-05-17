package io.jentz.winter.androidx.integration.test

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.Winter
import io.jentz.winter.androidx.AndroidPresentationScopeInjectionAdapter
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.androidx.inject.PresentationScope
import io.jentz.winter.androidx.viewmodel.viewModel
import io.jentz.winter.junit4.WinterRule
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ViewModelTest {

    private val adapter = AndroidPresentationScopeInjectionAdapter(Winter)

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

                    subcomponent(PresentationScope::class) {

                        prototype { SomeViewModelDependency() }

                        viewModel { TestViewModel(instance(), instance()) }

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
    fun should_provide_view_model() {
        scenario.onActivity { activity ->
            val viewModel0: TestViewModel = adapter.get(activity)!!.instance()
            val viewModel1: TestViewModel = adapter.get(activity)!!.instance()
            viewModel0.shouldBeSameInstanceAs(viewModel1)
        }
    }

}
