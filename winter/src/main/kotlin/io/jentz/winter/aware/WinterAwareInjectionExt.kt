package io.jentz.winter.aware

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph

/**
 * Create and return dependency graph for [this] by using [WinterAware.winterApplication].
 *
 * @param block An optional builder block to pass to the component createGraph method.
 * @return The newly created graph.
 * @throws [io.jentz.winter.WinterException] if [this] type is not supported.
 */
fun WinterAware.createGraph(block: ComponentBuilderBlock? = null): Graph =
    winterApplication.openGraph(this, block)

/**
 * Create and return dependency graph for [this] and inject all members into [instance] by using
 * [WinterAware.winterApplication].
 *
 * This is useful in conjunction with JSR330 `Inject` annotations.
 *
 * @param instance The instance to inject into.
 * @param block An optional builder block to pass to the component createGraph method.
 * @return The created dependency graph.
 * @throws [io.jentz.winter.WinterException] if [this] type is not supported.
 */
fun WinterAware.createGraphAndInject(
    instance: Any = this,
    block: ComponentBuilderBlock? = null
): Graph = createGraph(block).also { graph ->
    graph.inject(instance)
}

/**
 * Dispose the dependency graph of [this] by using [WinterAware.winterApplication].
 *
 * @throws [io.jentz.winter.WinterException] if type of [this] is not supported.
 */
fun WinterAware.disposeGraph() {
    winterApplication.closeGraph(this)
}

/**
 * Inject into [instance] by using the dependency graph of [this].
 *
 * @param instance The instance to inject dependencies into.
 * @throws [io.jentz.winter.WinterException] If given [instance] type is not supported.
 */
fun <T : Any> WinterAware.inject(instance: T) {
    graph.inject(instance)
}
