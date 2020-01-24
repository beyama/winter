package io.jentz.winter.evaluator

import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.plugin.Plugins

internal fun createServiceEvaluator(
    graph: Graph,
    component: Component,
    plugins: Plugins,
    checkForCyclicDependencies: Boolean
): ServiceEvaluator = when {
    component.requiresLifecycleCallbacks || plugins.isNotEmpty() -> {
        LifecycleServiceEvaluator(graph, plugins, checkForCyclicDependencies)
    }
    checkForCyclicDependencies -> {
        CyclicDependenciesCheckingDirectServiceEvaluator()
    }
    else -> {
        DirectServiceEvaluator()
    }
}
