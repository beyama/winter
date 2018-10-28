package io.jentz.winter.android.test.quotes

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import io.jentz.winter.Injection
import io.jentz.winter.WinterTree
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
    val activityTestRule = ActivityTestRule<QuotesActivity>(QuotesActivity::class.java, true, false)

    @get:Rule
    val winterTestRule = WinterTestRule.initializingComponent { _, builder ->
        if (builder.qualifier == "presentation") {
            builder.apply {
                singleton<ViewModel<QuotesViewState>>(generics = true, override = true) {
                    viewModel
                }
            }
        }
    }

    private val viewModel = TestViewModel<QuotesViewState>()

    @Test
    fun should_retain_presentation_scope_but_dispose_activity_scope_on_orientation_change() {
        activityTestRule.launchActivity(Intent())

        val activity = activityTestRule.activity

        val tree: WinterTree = Injection.getGraph(activity).instance()
        tree.has(QuotesActivity::class.java.name, activity).shouldBeTrue()
        val presentationGraph = tree.get(QuotesActivity::class.java.name)
        val activityGraph = tree.get(QuotesActivity::class.java.name, activity)

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

        val tree: WinterTree = Injection.getGraph(activityTestRule.activity).instance()
        val presentationGraph = tree.get(QuotesActivity::class.java.name)

        presentationGraph.isDisposed.shouldBeFalse()
        viewModel.isDisposed.shouldBeFalse()

        activityTestRule.finishActivity()

        waitForIt(timeoutMs = 5000) { presentationGraph.isDisposed }

        presentationGraph.isDisposed.shouldBeTrue()
        // WinterDisposablePlugin should dispose view model when graph gets disposed
        viewModel.isDisposed.shouldBeTrue()
    }

}
