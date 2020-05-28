package io.jentz.winter.evaluator

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.jentz.winter.Scope
import io.jentz.winter.emptyGraph
import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.Plugins
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class LifecycleServiceEvaluatorTest : AbstractCyclicServiceEvaluatorTest() {

    private val graph = emptyGraph()

    private val plugin: Plugin = mock()

    private val plugins = Plugins(plugin)

    override val evaluator = LifecycleServiceEvaluator(graph, plugins, true)

    @Test
    fun `should call service and plugin post-construct callbacks`() {
        val service = BoundTestService(evaluator) { "FOO" }
        evaluator
            .evaluate(service, graph)
            .shouldBe("FOO")

        service.postConstructCalled.shouldHaveSize(1)
        service.postConstructCalled.first().shouldBe("FOO")

        verify(plugin).postConstruct(graph, Scope.Prototype, "FOO")
    }

}
