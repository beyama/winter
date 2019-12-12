package io.jentz.winter.junit4

import io.jentz.winter.*
import io.kotlintest.matchers.boolean.shouldBeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.JUnitCore

class GraphLifecycleRuleTest {

    object TestApp : WinterApplication()

    class UnitTestWithRule {

        @get:Rule
        val rule = object : GraphLifecycleRule(TestApp) {}


        @Test
        fun `should register itself as plugin during test`() {
            TestApp.plugins.contains(rule).shouldBeTrue()
        }

    }

    @Test
    fun `should call all lifecycle methods during test`() {
        JUnitCore.runClasses(UnitTestWithRule::class.java).wasSuccessful().shouldBeTrue()
        TestApp.plugins.isEmpty().shouldBeTrue()
    }

}
