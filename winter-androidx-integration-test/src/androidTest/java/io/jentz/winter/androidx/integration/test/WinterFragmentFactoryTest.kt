package io.jentz.winter.androidx.integration.test

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.Winter
import io.jentz.winter.androidx.fragment.SimpleAndroidFragmentInjectionAdapter
import io.jentz.winter.androidx.fragment.WinterFragmentFactory
import io.jentz.winter.androidx.fragment.inject.FragmentScope
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.junit4.WinterRule
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class WinterFragmentFactoryTest {

    private val adapter = SimpleAndroidFragmentInjectionAdapter(Winter, enableWinterFragmentFactory = true)

    private val winterRule = WinterRule {
        testGraph(ActivityScope::class)
    }

    private val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

    private var factoryCalled: Boolean = false

    @get:Rule val rule: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() {
                val application: Application = Winter.graph.instance()
                Winter.closeGraph()

                Winter.component {
                    subcomponent(ActivityScope::class) {
                        prototype {
                            factoryCalled = true
                            TestFragment()
                        }
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
    fun should_resolve_fragment_by_class() {
        val graph = winterRule.requireTestGraph
        val factory: WinterFragmentFactory = graph.instance()
        factory.instance<TestFragment>().shouldBeInstanceOf<TestFragment>()
    }

    @Test
    fun should_resolve_fragment_on_recreate() {
        scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(TestFragment(), "test")
                .commit()
        }

        factoryCalled = false
        scenario.recreate()
        factoryCalled.shouldBeTrue()
    }

}
