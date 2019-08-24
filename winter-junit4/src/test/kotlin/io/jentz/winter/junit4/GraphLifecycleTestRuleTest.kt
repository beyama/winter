package io.jentz.winter.junit4

import io.jentz.winter.*
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.JUnitCore

class GraphLifecycleTestRuleTest {

    class UnitTestWithRule {
        companion object {
            var graphInitializingCalled = 0
            var graphInitializedCalled = 0
            var graphDisposeCalled = 0
            var postConstructCalled = 0
        }

        private val component = component {
            singleton { "" }
        }

        @get:Rule
        val rule = object : GraphLifecycleTestRule() {
            override fun graphInitializing(parentGraph: Graph?, builder: ComponentBuilder) {
                graphInitializingCalled += 1
            }

            override fun graphInitialized(graph: Graph) {
                graphInitializedCalled += 1
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
            graphInitializingCalled = 0
            graphInitializedCalled = 0
            postConstructCalled = 0
            graphDisposeCalled = 0

            val graph = component.createGraph()

            graphInitializingCalled.shouldBe(1)
            graphInitializedCalled.shouldBe(1)
            postConstructCalled.shouldBe(0)
            graphDisposeCalled.shouldBe(0)

            graph.instance<String>()

            graphInitializingCalled.shouldBe(1)
            graphInitializedCalled.shouldBe(1)
            postConstructCalled.shouldBe(1)
            graphDisposeCalled.shouldBe(0)

            graph.dispose()

            graphInitializingCalled.shouldBe(1)
            graphInitializedCalled.shouldBe(1)
            postConstructCalled.shouldBe(1)
            graphDisposeCalled.shouldBe(1)
        }

    }

    @Before
    fun beforeEach() {
        Winter.plugins.unregisterAll()
    }

    @Test
    fun `should call all lifecycle methods during test`() {
        JUnitCore.runClasses(UnitTestWithRule::class.java).wasSuccessful().shouldBeTrue()
        Winter.plugins.isEmpty().shouldBeTrue()
    }

}
