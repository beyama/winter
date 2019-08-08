package io.jentz.winter.aware

import io.jentz.winter.*

/**
 * Create and return dependency graph for [this] by using [WinterAware.injection].
 *
 * @param block An optional builder block to pass to the component init method.
 * @return The newly created graph.
 * @throws [io.jentz.winter.WinterException] if [this] type is not supported.
 */
fun WinterAware.createGraph(block: ComponentBuilderBlock? = null): Graph =
    injection.createGraph(this, block)

/**
 * Create and return dependency graph for [this] and also pass the graph to the given
 * [injector] by using [WinterAware.injection].
 *
 * @param injector The [Injector] to inject into.
 * @param block An optional builder block to pass to the component init method.
 * @return The created dependency graph.
 * @throws [io.jentz.winter.WinterException] if [this] type is not supported.
 */
fun WinterAware.createGraphAndInject(
    injector: Injector,
    block: ComponentBuilderBlock? = null
): Graph = injection.createGraphAndInject(this, injector, block)

/**
 * Create and return dependency graph for [this] and inject all members into [instance] by using
 * [WinterAware.injection].
 *
 * This is useful in conjunction with JSR330 `Inject` annotations.
 *
 * @param instance The instance to inject into.
 * @param injectSuperClasses If true this will look for members injectors for super classes too.
 * @param block An optional builder block to pass to the component init method.
 * @return The created dependency graph.
 * @throws [io.jentz.winter.WinterException] if [this] type is not supported.
 */
fun <T : Any> WinterAware.createGraphAndInject(
    instance: T,
    injectSuperClasses: Boolean = false,
    block: ComponentBuilderBlock? = null
): Graph = createGraph(block).also { graph ->
    graph.inject(instance, injectSuperClasses)
}

/**
 * Dispose the dependency graph of [this] by using [WinterAware.injection].
 *
 * @throws [io.jentz.winter.WinterException] if type of [this] is not supported.
 */
fun WinterAware.disposeGraph() {
    injection.disposeGraph(this)
}

/**
 * Get dependency graph for [this] and inject dependencies into [injector] by using
 * [WinterAware.injection].
 *
 * @param injector The [Injector] to inject into.
 * @throws [io.jentz.winter.WinterException] if type of [this] is not supported.
 */
fun WinterAware.inject(injector: Injector) {
    injection.inject(this, injector)
}

/**
 * Inject into [instance] by using the dependency graph of [this] by using [WinterAware.injection].
 * This is useful in conjunction with Winters JSR330 annotation processor.
 *
 * @param instance The instance to retrieve the dependency graph for and inject dependencies
 *                 into.
 * @param injectSuperClasses If true this will look for members injectors for super classes too.
 * @throws [io.jentz.winter.WinterException] If given [instance] type is not supported.
 */
fun <T : Any> WinterAware.inject(instance: T, injectSuperClasses: Boolean = false) {
    graph.inject(instance, injectSuperClasses)
}
