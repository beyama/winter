package io.jentz.winter.androidx.integration.test

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.androidx.AndroidPresentationScopeInjectionAdapter
import io.jentz.winter.androidx.fragment.AndroidPresentationScopeFragmentInjectionAdapter
import io.jentz.winter.androidx.fragment.SimpleAndroidFragmentInjectionAdapter
import io.jentz.winter.androidx.fragment.useAndroidPresentationScopeFragmentAdapter
import io.jentz.winter.androidx.fragment.useSimpleAndroidFragmentAdapter
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.androidx.inject.PresentationScope
import io.jentz.winter.androidx.useAndroidPresentationScopeAdapter
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
class AndroidPresentationScopeFragmentInjectionAdapterTest {

    private val adapter = AndroidPresentationScopeFragmentInjectionAdapter(Winter)

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
        val app = WinterApplication().apply { useAndroidPresentationScopeFragmentAdapter() }
        app.injectionAdapter.shouldBeInstanceOf<AndroidPresentationScopeFragmentInjectionAdapter>()
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
        Espresso.onIdle()

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
