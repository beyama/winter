package io.jentz.winter.android.test.quotes

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Graph
import io.jentz.winter.GraphRegistry
import io.jentz.winter.android.test.R
import io.jentz.winter.android.test.isDisplayed
import io.jentz.winter.android.test.isNotDisplayed
import io.jentz.winter.android.test.model.QuoteRepository
import io.jentz.winter.android.test.viewmodel.TestViewModel
import io.jentz.winter.android.test.viewmodel.ViewModel
import io.jentz.winter.android.test.waitForIt
import io.jentz.winter.junit4.WinterTestRule
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class QuotesActivityTest {

    @get:Rule
    val activityTestRule = ActivityTestRule(QuotesActivity::class.java, true, false)

    @get:Rule
    val winterTestRule = object : WinterTestRule() {
        override fun initializingComponent(parentGraph: Graph?, builder: ComponentBuilder) {
            if (builder.qualifier == "presentation") {
                builder.apply {
                    singleton<ViewModel<QuotesViewState>>(
                        generics = true,
                        override = true
                    ) { viewModel }
                }
            }
        }
    }

    private val viewModel = TestViewModel<QuotesViewState>()

    @Test
    fun should_retain_presentation_scope_but_dispose_activity_scope_on_orientation_change() {
        activityTestRule.launchActivity(Intent())

        val activity = activityTestRule.activity

        GraphRegistry.has(QuotesActivity::class.java, activity).shouldBeTrue()
        val presentationGraph = GraphRegistry.get(QuotesActivity::class.java)
        val activityGraph = GraphRegistry.get(QuotesActivity::class.java, activity)

        viewModel.downstream.onNext(QuotesViewState(isLoading = true))

        onView(withId(R.id.progressIndicatorView)).isDisplayed()

        viewModel.downstream.onNext(
            QuotesViewState(
                isLoading = false,
                quotes = QuoteRepository.quotes
            )
        )

        onView(withId(R.id.progressIndicatorView)).isNotDisplayed()

        activityTestRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        onView(withId(R.id.progressIndicatorView)).isNotDisplayed()

        presentationGraph.isDisposed.shouldBeFalse()
        activityGraph.isDisposed.shouldBeTrue()
    }

    @Test
    fun should_dispose_presentation_scope_and_dispose_view_model_when_activity_finishes() {
        activityTestRule.launchActivity(Intent())

        val presentationGraph = GraphRegistry.get(QuotesActivity::class.java)

        presentationGraph.isDisposed.shouldBeFalse()
        viewModel.isDisposed.shouldBeFalse()

        activityTestRule.finishActivity()

        waitForIt(timeoutMs = 5000) { presentationGraph.isDisposed }

        presentationGraph.isDisposed.shouldBeTrue()
        // WinterDisposablePlugin should dispose view model when graph gets disposed
        viewModel.isDisposed.shouldBeTrue()
    }

}
