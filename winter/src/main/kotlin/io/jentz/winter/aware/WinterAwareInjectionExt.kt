package io.jentz.winter.aware

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.Injector

/**
 * Create and return dependency graph for [instance].
 *
 * @param block An optional builder block to pass to the component init method.
 * @return The newly created graph.
 * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
 */
fun WinterAware.createDependencyGraph(block: ComponentBuilderBlock? = null): Graph =
    winterInjection.createGraph(this, block)

/**
 * Create and return dependency graph for [this] and also pass the graph to the given
 * [injector].
 *
 * @param injector The injector to inject into.
 * @param block An optional builder block to pass to the component init method.
 * @return The created dependency graph.
 * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
 */
fun WinterAware.createDependencyGraphAndInject(
    injector: Injector,
    block: ComponentBuilderBlock? = null
): Graph = winterInjection.createGraphAndInject(this, injector, block)

/**
 * Dispose the dependency graph of [this].
 *
 * @throws [io.jentz.winter.WinterException] if type of [this] is not supported.
 */
fun WinterAware.disposeDependencyGraph() {
    winterInjection.disposeGraph(this)
}

/**
 * Get dependency graph for [this] and inject dependencies into injector.
 *
 * @param injector The injector to inject into.
 * @throws [io.jentz.winter.WinterException] if type of [this] is not supported.
 */
fun WinterAware.inject(injector: Injector) {
    winterInjection.inject(this, injector)
}
