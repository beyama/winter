package io.jentz.winter

import io.jentz.winter.adapter.ApplicationGraphOnlyAdapter

/**
 * Abstraction to create, get and dispose a dependency graph from a class that can't make use of
 * constructor injection.
 *
 * For applications it is recommended to use the object version [Injection] directly and for
 * libraries it is recommended to create a object version from [WinterInjection] instead.
 *
 * @see Injection
 */
open class WinterInjection {

    /**
     * Adapter interface to provide application specific graph creation and retrieval strategy.
     */
    interface Adapter {

        /**
         * Get dependency graph for [instance].
         *
         * @param instance The instance to get the graph for.
         * @return The graph for [instance].
         * @throws [io.jentz.winter.WinterException] if no graph for [instance] exists.
         *
         */
        fun getGraph(instance: Any): Graph

        /**
         * Create dependency graph for [instance].
         *
         * The adapter implementation is responsible for storing the created graph.
         *
         * @param instance The instance to create a dependency graph for.
         * @param block An optional builder block to pass to the component createGraph method.
         * @return The newly created graph
         * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
         *
         */
        fun createGraph(instance: Any, block: ComponentBuilderBlock?): Graph

        /**
         * Dispose the dependency graph of the given [instance].
         *
         * @param instance The instance to dispose the graph for.
         * @throws [io.jentz.winter.WinterException] if no graph for this [instance] type exists.
         */
        fun disposeGraph(instance: Any)
    }

    /**
     * Set the application specific [adapter][Adapter].
     *
     * Default adapter is [ApplicationGraphOnlyAdapter] that operates on [Winter].
     */
    var adapter: Adapter = ApplicationGraphOnlyAdapter(Winter.tree)

    /**
     * Create and return dependency graph for [instance].
     *
     * @param instance The instance for which a graph should be created.
     * @param block An optional builder block to pass to the component createGraph method.
     * @return The newly created graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun createGraph(instance: Any, block: ComponentBuilderBlock? = null): Graph =
        adapter.createGraph(instance, block)

    /**
     * Create and return dependency graph for [instance] and also pass the graph to the given
     * [injector].
     *
     * @param instance The instance for which a graph should be created.
     * @param injector The injector to inject into.
     * @param block An optional builder block to pass to the component createGraph method.
     * @return The created dependency graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun createGraphAndInject(
        instance: Any,
        injector: Injector,
        block: ComponentBuilderBlock? = null
    ): Graph = createGraph(instance, block).also(injector::inject)

    /**
     * Create and return dependency graph for [instance] and inject all members into instance.
     *
     * This is useful in conjunction with JSR330 `Inject` annotations.
     *
     * @param instance The instance to create a graph for and to inject into.
     * @param block An optional builder block to pass to the component createGraph method.
     * @return The created dependency graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun <T : Any> createGraphAndInject(
        instance: T,
        block: ComponentBuilderBlock? = null
    ): Graph = createGraph(instance, block).also { graph ->
        graph.inject(instance)
    }

    /**
     * Get dependency graph for [instance].
     *
     * @param instance The instance to retrieve the dependency graph for.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     *
     */
    fun getGraph(instance: Any): Graph = adapter.getGraph(instance)

    /**
     * Dispose the dependency graph of the given [instance].
     *
     * @param instance The instance to dispose the graph for.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun disposeGraph(instance: Any) {
        adapter.disposeGraph(instance)
    }

    /**
     * Get dependency graph for given [instance] and inject dependencies into injector.
     *
     * @param instance The instance to retrieve the dependency graph for.
     * @param injector The injector to inject into.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun inject(instance: Any, injector: Injector) {
        injector.inject(getGraph(instance))
    }

    /**
     * Inject into [instance] by using the dependency graph of the [instance].
     * This uses [MembersInjector] and is useful in conjunction with Winters JSR330 annotation
     * processor.
     *
     * @param instance The instance to retrieve the dependency graph for and inject dependencies
     *                 into.
     * @throws [io.jentz.winter.WinterException] If given [instance] type is not supported.
     */
    fun <T : Any> inject(instance: T) {
        getGraph(instance).inject(instance)
    }

}
