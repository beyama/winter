package io.jentz.winter.androidx.integration.test

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.fragment.app.FragmentManager
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.androidx.fragment.SimpleAndroidFragmentInjectionAdapter
import io.jentz.winter.androidx.fragment.useSimpleAndroidFragmentAdapter
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.junit4.WinterRule
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
class SimpleAndroidFragmentInjectionAdapterTest {

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
        val app = WinterApplication().apply { useSimpleAndroidFragmentAdapter() }
        app.injectionAdapter.shouldBeInstanceOf<SimpleAndroidFragmentInjectionAdapter>()
    }

    @Test
    fun should_get_activity_graph_for_fragment() {
        val fragment = TestFragment()
        scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction().apply {
                add(fragment, "test")
                commit()
            }
        }
        onIdle()

        winterRule.requireTestGraph.shouldBeSameInstanceAs(adapter.get(fragment))
    }

    @Test
    fun should_provide_android_types() {
        val graph = winterRule.requireTestGraph
        scenario.onActivity { activity ->
            graph.instance<FragmentManager>()
                .shouldBeSameInstanceAs(activity.supportFragmentManager)

            graph.instance<SavedStateRegistryOwner>()
                .shouldBeSameInstanceAs(activity)

            graph.instance<SavedStateRegistry>()
                .shouldBeSameInstanceAs(activity.savedStateRegistry)

            graph.instance<OnBackPressedDispatcherOwner>()
                .shouldBeSameInstanceAs(activity)

            graph.instance<OnBackPressedDispatcher>()
                .shouldBeSameInstanceAs(activity.onBackPressedDispatcher)

            graph.instance<ComponentActivity>()
                .shouldBeSameInstanceAs(activity)
        }
    }

}
