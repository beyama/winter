package io.jentz.winter.junit4

import io.jentz.winter.*
import io.jentz.winter.plugin.SimplePlugin
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.Before
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
        val rule = object : WinterTestRule() {
            override fun initializingComponent(parentGraph: Graph?, builder: ComponentBuilder) {
                initializingComponentCalled += 1
            }

            override fun postConstruct(graph: Graph, scope: Scope, argument: Any, instance: Any) {
                postConstructCalled += 1
            }

            override fun graphDispose(graph: Graph) {
                graphDisposeCalled += 1
            }
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

    @Before
    fun beforeEach() {
        Winter.plugins.unregisterAll()
    }

    @Test
    fun `should call each plugin once per test with constructor`() {
        JUnitCore.runClasses(UsedWithConstructor::class.java).wasSuccessful().shouldBeTrue()
        Winter.plugins.isEmpty().shouldBeTrue()
    }

}
