package io.jentz.winter.androidx.integration.test

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.Winter
import io.jentz.winter.androidx.SimpleAndroidInjectionAdapter
import io.jentz.winter.androidx.fragment.SimpleAndroidFragmentInjectionAdapter
import io.jentz.winter.androidx.fragment.inject.FragmentScope
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.junit4.WinterRule
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedstateViewModelTest {

    private val adapter = SimpleAndroidFragmentInjectionAdapter(Winter)

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

                    prototype { TestSavedStateViewModel(instance(), instance(), instance()) }

                    subcomponent(ActivityScope::class) {
                        subcomponent(FragmentScope::class) {
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
    fun should_provide_view_model_to_activity() {
        scenario.onActivity { activity ->
            activity.savedStatViewModel.shouldBeInstanceOf<TestSavedStateViewModel>()

            activity.savedStatViewModel.id.shouldBe("Test ID")

            ViewModelProvider(activity).get(TestSavedStateViewModel::class.java)
                .shouldBeSameInstanceAs(activity.savedStatViewModel)
        }
    }

    @Test
    fun should_provide_view_model_to_fragment() {
        val fragment = TestFragment()
        scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction().apply {
                add(fragment, "test")
                commit()
            }
        }

        onIdle()

        scenario.onActivity { activity ->
            fragment.savedStateViewModel.let { vm ->
                vm.shouldBeInstanceOf<TestSavedStateViewModel>()
                vm.shouldNotBeSameInstanceAs(activity.savedStatViewModel)
                vm.id.shouldBe("Fragment Test ID")

                ViewModelProvider(fragment).get(TestSavedStateViewModel::class.java)
                    .shouldBeSameInstanceAs(vm)
            }
        }
    }

    @Test
    fun should_provide_activity_view_model_to_fragment() {
        val fragment = TestFragment()
        scenario.onActivity { activity ->
            activity.savedStatViewModel // initialize lazy view model before retrieving it in fragment

            activity.supportFragmentManager.beginTransaction().apply {
                add(fragment, "test")
                commit()
            }
        }

        onIdle()

        scenario.onActivity { activity ->
            fragment.activitySavedStateViewModel.let { vm ->
                vm.shouldBeSameInstanceAs(activity.savedStatViewModel)
            }
        }
    }

}
