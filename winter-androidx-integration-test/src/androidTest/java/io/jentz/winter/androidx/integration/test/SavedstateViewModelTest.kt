package io.jentz.winter.androidx.integration.test

import android.app.Activity
import android.app.Application
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.Winter
import io.jentz.winter.androidx.SimpleAndroidInjectionAdapter
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.androidx.viewmodel.savedstate.savedStateViewModel
import io.jentz.winter.junit4.WinterRule
import io.jentz.winter.typeKey
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedstateViewModelTest {

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
                        alias(typeKey<Activity>(), typeKey<SavedStateRegistryOwner>())
                        savedStateViewModel { TestSavedstateViewModel(instance(), instance()) }
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
            val viewModel0: TestSavedstateViewModel = adapter.get(activity)!!.instance()
            val viewModel1: TestSavedstateViewModel = adapter.get(activity)!!.instance()
            viewModel0.shouldBeSameInstanceAs(viewModel1)
        }
    }

}
