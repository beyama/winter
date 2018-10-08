package io.jentz.winter.android.test.quotes

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import io.jentz.winter.GraphRegistry
import io.jentz.winter.android.test.R
import io.jentz.winter.android.test.isDisplayed
import io.jentz.winter.android.test.isNotDisplayed
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

    @Test
    fun should_retain_presentation_scope_but_dispose_activity_scope_on_orientation_change() {
        activityTestRule.launchActivity(Intent())

        val activity = activityTestRule.activity

        GraphRegistry.has(QuotesActivity::class.java.name, activity).shouldBeTrue()
        val presentationGraph = GraphRegistry.get(QuotesActivity::class.java.name)
        val activityGraph = GraphRegistry.get(QuotesActivity::class.java.name, activity)

        onView(withId(R.id.progressIndicatorView)).isDisplayed()

        Thread.sleep(QuotesViewModel.FAKE_NETWORK_DELAY)

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

        val presentationGraph = GraphRegistry.get(QuotesActivity::class.java.name)
        val viewModel: QuotesViewModel = presentationGraph.instance()

        presentationGraph.isDisposed.shouldBeFalse()
        viewModel.isDisposed.shouldBeFalse()

        Espresso.pressBackUnconditionally()

        presentationGraph.isDisposed.shouldBeTrue()
        // WinterDisposablePlugin should dispose view model when graph gets disposed
        viewModel.isDisposed.shouldBeTrue()
    }


}