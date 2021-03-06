package io.jentz.winter.android.sample.quotes

import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.jentz.winter.android.sample.R
import io.jentz.winter.android.sample.isDisplayed
import io.jentz.winter.android.sample.isNotDisplayed
import io.jentz.winter.android.sample.model.QuoteRepository
import io.jentz.winter.android.sample.viewmodel.TestViewModel
import io.jentz.winter.android.sample.viewmodel.ViewModel
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.androidx.inject.PresentationScope
import io.jentz.winter.junit4.WinterRule
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class QuotesActivityTest {

    private val winterRule = WinterRule(this) {
        extend(PresentationScope::class) {
            singleton<ViewModel<QuotesViewState>>(generics = true, override = true) { viewModel }
        }
        testGraph(ActivityScope::class)
    }

    private val activityScenarioRule = ActivityScenarioRule<QuotesActivity>(QuotesActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(winterRule).around(activityScenarioRule)

    private val viewModel = TestViewModel<QuotesViewState>()

    @Test
    fun should_retain_presentation_scope_but_close_activity_scope_on_orientation_change() {
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

        activityScenarioRule.scenario.recreate()

        onView(withId(R.id.progressIndicatorView)).isNotDisplayed()

        presentationGraph.isClosed.shouldBeFalse()
        activityGraph.isClosed.shouldBeTrue()
    }

    @Test
    fun should_close_presentation_scope_and_dispose_view_model_when_activity_finishes() {
        val presentationGraph = winterRule.requireTestGraph.parent!!

        presentationGraph.isClosed.shouldBeFalse()
        viewModel.isDisposed.shouldBeFalse()

        activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)

        presentationGraph.isClosed.shouldBeTrue()
        // WinterDisposablePlugin should dispose view model when graph gets closed
        viewModel.isDisposed.shouldBeTrue()
    }

}
