package io.jentz.winter.android.test

import androidx.test.espresso.DataInteraction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers

fun ViewInteraction.isNotDisplayed() = check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
fun ViewInteraction.isDisplayed() = check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
fun ViewInteraction.click() = perform(ViewActions.click())
fun DataInteraction.click() = perform(ViewActions.click())