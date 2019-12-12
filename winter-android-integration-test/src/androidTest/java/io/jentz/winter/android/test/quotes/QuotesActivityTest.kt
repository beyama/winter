package io.jentz.winter.android.test.quotes

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.android.test.R
import io.jentz.winter.android.test.isDisplayed
import io.jentz.winter.android.test.isNotDisplayed
import io.jentz.winter.android.test.model.QuoteRepository
import io.jentz.winter.android.test.viewmodel.TestViewModel
import io.jentz.winter.android.test.viewmodel.ViewModel
import io.jentz.winter.android.test.waitForIt
import io.jentz.winter.junit4.WinterRule
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class QuotesActivityTest {

    private val winterRule = WinterRule(this) {
        extend("presentation") {
            singleton<ViewModel<QuotesViewState>>(generics = true, override = true) { viewModel }
        }
        testGraph("activity")
    }

    private val activityScenarioRule = ActivityScenarioRule<QuotesActivity>(QuotesActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(winterRule).around(activityScenarioRule)

    private val viewModel = TestViewModel<QuotesViewState>()

    private lateinit var activity: QuotesActivity

    private lateinit var scenario: ActivityScenario<QuotesActivity>

    @Before
    fun beforeEach() {
        scenario = activityScenarioRule.scenario
        activity = winterRule.requireTestGraph.instance<Activity>() as QuotesActivity
    }

    @Test
    fun should_retain_presentation_scope_but_dispose_activity_scope_on_orientation_change() {
        val activityGraph = winterRule.requireTestGraph
        val presentationGraph = activityGraph.parent!!

        viewModel.downstream.onNext(QuotesViewState(isLoading = true))

        onView(withId(R.id.progressIndicatorView)).isDisplayed()

        viewModel.downstream.onNext(
            QuotesViewState(
                isLoading = false,
                quotes = QuoteRepository.quotes
            )
        )

        onView(withId(R.id.progressIndicatorView)).isNotDisplayed()

        scenario.recreate()

        onView(withId(R.id.progressIndicatorView)).isNotDisplayed()

        presentationGraph.isDisposed.shouldBeFalse()
        activityGraph.isDisposed.shouldBeTrue()
    }

    @Test
    fun should_dispose_presentation_scope_and_dispose_view_model_when_activity_finishes() {
        val presentationGraph = winterRule.requireTestGraph.parent!!

        presentationGraph.isDisposed.shouldBeFalse()
        viewModel.isDisposed.shouldBeFalse()

        activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)

        waitForIt(timeoutMs = 5000) { presentationGraph.isDisposed }

        presentationGraph.isDisposed.shouldBeTrue()
        // WinterDisposablePlugin should dispose view model when graph gets disposed
        viewModel.isDisposed.shouldBeTrue()
    }

}
