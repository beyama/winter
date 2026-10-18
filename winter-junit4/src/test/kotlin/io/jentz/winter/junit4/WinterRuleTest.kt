package io.jentz.winter.junit4

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.jentz.winter.WinterApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.JUnitCore

class WinterRuleTest {

    private object TestApp : WinterApplication()

    class EachRunnerTest {

        @get:Rule val rule = WinterRule(TestApp) {}

        @Test
        fun `session plugin should be registered`() {
            assertThat(TestApp.plugins.size).isEqualTo(1)
        }

    }

    @Test
    fun `should unregister plugin after test`() {
        assertThat(JUnitCore.runClasses(EachRunnerTest::class.java).wasSuccessful()).isTrue()
        assertThat(TestApp.plugins).isEmpty()
    }

}
