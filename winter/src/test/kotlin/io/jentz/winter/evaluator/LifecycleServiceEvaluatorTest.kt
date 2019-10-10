package io.jentz.winter.evaluator

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.jentz.winter.*
import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.Plugins
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.eclipse.jgit.merge.StrategySimpleTwoWayInCore
import org.junit.jupiter.api.Test

class LifecycleServiceEvaluatorTest : AbstractCyclicServiceEvaluatorTest() {

    private val graph = emptyGraph()

    private val plugin: Plugin = mock()

    private val plugins = Plugins(plugin)

    override val evaluator = LifecycleServiceEvaluator(graph, plugins, true)

    @Test
    fun `should call service and plugin post-construct callbacks`() {
        val service = BoundTestService(evaluator) { it.toUpperCase() }
        evaluator
            .evaluate(service, "foo")
            .shouldBe("FOO")

        service.postConstructCalled.shouldHaveSize(1)
        service.postConstructCalled.first().shouldBe("foo" to "FOO")

        verify(plugin).postConstruct(graph, Scope.Prototype, "foo", "FOO")
    }

}
