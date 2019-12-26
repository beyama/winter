package io.jentz.winter.junit4

import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.JUnitCore
import javax.inject.Inject

class WinterRuleTest {

    private class Dependency

    private class Service {
        @Inject var dependency: Dependency? = null
    }

    private object TestApp : WinterApplication(block = {
        constant(Dependency())
    })

    class EachRunnerTest {

        @get:Rule val rule = WinterRule {
            application = TestApp
        }

        @Test
        fun `session plugin should be registered`() {
            TestApp.plugins.size.shouldBe(1)
        }

        @Test
        fun `should inject into target by using reflection`() {
            TestApp.tree.create()
            Service().apply {
                rule.inject(this)
                dependency.shouldNotBeNull()
            }
        }

    }

    @Test
    fun `should unregister plugin after test`() {
        JUnitCore.runClasses(EachRunnerTest::class.java).wasSuccessful().shouldBeTrue()
        TestApp.plugins.isEmpty().shouldBeTrue()
    }

}
