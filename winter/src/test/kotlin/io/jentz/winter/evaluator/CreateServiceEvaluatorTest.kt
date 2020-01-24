package io.jentz.winter.evaluator

import com.nhaarman.mockitokotlin2.mock
import io.jentz.winter.emptyGraph
import io.jentz.winter.graph
import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.Plugins
import io.kotlintest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class CreateServiceEvaluatorTest {

    private val graph = emptyGraph()

    private val plugin = mock<Plugin>()

    private val plugins = Plugins()

    @Test
    fun `should return DirectServiceEvaluator if no cyclic test and now lifecycle methods are required`() {
        createServiceEvaluator(graph, graph.component, plugins, false)
            .shouldBeInstanceOf<DirectServiceEvaluator>()
    }

    @Test
    fun `should return CyclicDependenciesCheckingDirectServiceEvaluator if cyclic test is required but no lifecycle methods`() {
        createServiceEvaluator(graph, graph.component, plugins, true)
            .shouldBeInstanceOf<CyclicDependenciesCheckingDirectServiceEvaluator>()
    }

    @Test
    fun `should return LifecycleServiceEvaluator if a plugin is registered`() {
        createServiceEvaluator(graph, graph.component, plugins + plugin, false)
            .shouldBeInstanceOf<LifecycleServiceEvaluator>()
    }

    @Test
    fun `should return LifecycleServiceEvaluator if a component requires lifecycle methods`() {
        val graph = graph { singleton(onPostConstruct = {}) { Any() } }
        createServiceEvaluator(graph, graph.component, plugins, false)
            .shouldBeInstanceOf<LifecycleServiceEvaluator>()
    }

    @Test
    fun `should return LifecycleServiceEvaluator if a component requires lifecycle methods and plugins are registered`() {
        val graph = graph { singleton(onPostConstruct = {}) { Any() } }
        createServiceEvaluator(graph, graph.component, plugins + plugin, false)
            .shouldBeInstanceOf<LifecycleServiceEvaluator>()
    }

}
