package io.jentz.winter.junit4

import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.JUnitCore

class WinterRuleTest {

    private object TestApp : WinterApplication(block = {})

    class EachRunnerTest {

        @get:Rule val rule = WinterRule {
            application = TestApp
        }

        @Test
        fun `session plugin should be registered`() {
            TestApp.plugins.size.shouldBe(1)
        }

    }

    @Test
    fun `should unregister plugin after test`() {
        JUnitCore.runClasses(EachRunnerTest::class.java).wasSuccessful().shouldBeTrue()
        TestApp.plugins.isEmpty().shouldBeTrue()
    }

}
