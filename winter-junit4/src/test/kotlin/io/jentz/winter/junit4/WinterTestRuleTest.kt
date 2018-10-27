package io.jentz.winter.junit4

import io.jentz.winter.component
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.JUnitCore

class WinterTestRuleTest {

    class UsedWithConstructor {
        companion object {
            var initializingComponentCalled = 0
            var postConstructCalled = 0
            var graphDisposeCalled = 0
        }

        private val component = component {
            singleton { "" }
        }

        @get:Rule
        val rule = WinterTestRule(
            initializingComponentPlugin = { _, _ ->
                initializingComponentCalled += 1
            },
            postConstructPlugin = { _, _, _, _ ->
                postConstructCalled += 1
            },
            graphDisposePlugin = { _ ->
                graphDisposeCalled += 1
            }
        )

        @Test
        fun test() {
            initializingComponentCalled = 0
            postConstructCalled = 0
            graphDisposeCalled = 0

            val graph = component.init()

            initializingComponentCalled.shouldBe(1)
            postConstructCalled.shouldBe(0)
            graphDisposeCalled.shouldBe(0)

            graph.instance<String>()

            initializingComponentCalled.shouldBe(1)
            postConstructCalled.shouldBe(1)
            graphDisposeCalled.shouldBe(0)

            graph.dispose()

            initializingComponentCalled.shouldBe(1)
            postConstructCalled.shouldBe(1)
            graphDisposeCalled.shouldBe(1)
        }

    }

    class UsedWithCompanionMethod {
        companion object {
            var initializingComponentCalled = 0
            var postConstructCalled = 0
            var graphDisposeCalled = 0
        }

        private val component = component {
            singleton { "" }
        }

        @get:Rule
        val rule1 = WinterTestRule.initializingComponent { _, _ ->
            initializingComponentCalled += 1
        }

        @get:Rule
        val rule2 = WinterTestRule.postConstruct { _, _, _, _ ->
            postConstructCalled += 1
        }

        @get:Rule
        val rule3 = WinterTestRule.graphDispose {
            graphDisposeCalled += 1
        }

        @Test
        fun test() {
            initializingComponentCalled = 0
            postConstructCalled = 0
            graphDisposeCalled = 0

            val graph = component.init()

            initializingComponentCalled.shouldBe(1)
            postConstructCalled.shouldBe(0)
            graphDisposeCalled.shouldBe(0)

            graph.instance<String>()

            initializingComponentCalled.shouldBe(1)
            postConstructCalled.shouldBe(1)
            graphDisposeCalled.shouldBe(0)

            graph.dispose()

            initializingComponentCalled.shouldBe(1)
            postConstructCalled.shouldBe(1)
            graphDisposeCalled.shouldBe(1)
        }

    }

    @Test
    fun `should call each plugin once per test with constructor`() {
        JUnitCore.runClasses(UsedWithConstructor::class.java).wasSuccessful().shouldBeTrue()
    }

    @Test
    fun `should call each plugin once per test with companion method`() {
        JUnitCore.runClasses(UsedWithCompanionMethod::class.java).wasSuccessful().shouldBeTrue()
    }

}
