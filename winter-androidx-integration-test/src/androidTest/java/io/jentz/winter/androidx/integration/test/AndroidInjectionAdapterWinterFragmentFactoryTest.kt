package io.jentz.winter.androidx.integration.test

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
import io.jentz.winter.androidx.DependencyGraphContextWrapper
import io.jentz.winter.androidx.SimpleAndroidInjectionAdapter
import io.jentz.winter.androidx.WinterFragmentFactory
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.emptyGraph
import io.jentz.winter.junit4.WinterRule
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidInjectionAdapterWinterFragmentFactoryTest {

    private val adapter = SimpleAndroidInjectionAdapter(Winter, enableWinterFragmentFactory = true)

    private val winterRule = WinterRule {
        testGraph(ActivityScope::class)
    }

    private val activityScenarioRule = ActivityScenarioRule<TestActivity>(TestActivity::class.java)

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
    fun should_setup_fragment_factory() {
        val graph = winterRule.requireTestGraph
        val fragmentManager: FragmentManager = graph.instance()
        val factory: WinterFragmentFactory = graph.instance()
        fragmentManager.fragmentFactory.shouldBeSameInstanceAs(factory)
    }

}
