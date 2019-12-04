package io.jentz.winter.junit4

import io.jentz.winter.WinterApplication
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.Test
import org.junit.runner.JUnitCore
import org.junit.runner.RunWith

class WinterRunnerTest {

    private object TestApp : WinterApplication()

    class CustomTestRunner(klass: Class<*>) : WinterRunner(klass, {
        application = TestApp
    })

    @RunWith(CustomTestRunner::class)
    class EachRunnerTest {

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
