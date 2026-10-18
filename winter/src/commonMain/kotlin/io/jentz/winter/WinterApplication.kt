package io.jentz.winter

import io.jentz.winter.plugin.Plugins

/**
 * Holds the [plugins], the application [Component] and the application [Graph].
 *
 * Applications usually use the [Winter] object. To use Winter in a library it is recommended to
 * create a library specific object from [WinterApplication], so that the library doesn't share
 * its component, graph and plugins with the application.
 *
 * Example:
 *
 * ```
 * // configure the application component
 * Winter.component {
 *   singleton<GitHubApi> { GitHubApiImpl() }
 *   singleton { RepoListViewModel(instance()) }
 * }
 * // open the application graph
 * val graph = Winter.openGraph()
 * ```
 *
 * Classes that can't use constructor injection (e.g. Android activities) can retrieve their
 * dependencies from the application graph with the property delegates of the winter-delegate
 * module.
 */
open class WinterApplication() {

    private val lock = newLock(this)

    /**
     * Convenient constructor to configure the application [Component] during initialization.
     *
     * Example:
     * ```
     * object MyLibApp : WinterApplication {
     *   // ... declaration of dependencies
     * }
     * ```
     *
     * @param qualifier A qualifier for the component.
     * @param block The component builder block.
     */
    constructor(
        qualifier: Qualifier = ApplicationScope,
        block: ComponentBuilderBlock
    ) : this() {
        component(qualifier, block)
    }

    /**
     * Get the application graph if open otherwise null.
     */
    var graphOrNull: Graph? = null
        private set

    /**
     * Get the application graph.
     *
     * @throws WinterException If application graph is not open.
     */
    val graph: Graph
        get() = graphOrNull ?: throw WinterException(
            "Application graph is not open."
        )

    /**
     * The application component.
     */
    var component: Component = Component.EMPTY
        set(value) {
            lock.withLock {
                if (graphOrNull != null) {
                    throw WinterException(
                        "Cannot set component because application graph is already open."
                    )
                }
                field = value
            }
        }

    /**
     * The plugins registered on the application.
     */
    var plugins: Plugins = Plugins.EMPTY

    /**
     * Sets the application component by supplying an optional qualifier and a component builder
     * block.
     *
     * @param qualifier The qualifier for the new component.
     * @param block The component builder block.
     */
    fun component(qualifier: Qualifier = ApplicationScope, block: ComponentBuilderBlock) {
        this.component = io.jentz.winter.component(qualifier, block)
    }

    /**
     * Open the application component.
     *
     * @param block Optional builder block to derive the application component.
     *
     * @return The newly opened application graph.
     */
    fun openGraph(block: ComponentBuilderBlock? = null): Graph = lock.withLock {
        if (graphOrNull != null) {
            throw WinterException("Cannot open application graph because it is already open.")
        }
        openInternal(block)
    }

    /**
     * Get application graph if already open otherwise open and return it.
     *
     * @param block Optional builder block to derive the application component.
     *
     * @return The application graph.
     */
    fun getOrOpenGraph(block: ComponentBuilderBlock? = null): Graph = lock.withLock {
        graphOrNull?.let { return it }
        openInternal(block)
    }

    /**
     * Create graph from [component] without registering it as application graph.
     *
     * @param block Optional builder block to derive the application component.
     *
     * @return The application graph.
     */
    fun createGraph(block: ComponentBuilderBlock? = null): Graph =
        component.createGraph(this, block)

    /**
     * Close the application graph.
     *
     * @throws WinterException When no graph was found in path.
     */
    fun closeGraph() {
        val graph = graphOrNull ?: throw WinterException(
            "Cannot close because noting is open."
        )
        graph.close()
    }

    /**
     * Close the application graph if open otherwise do nothing.
     */
    fun closeGraphIfOpen() {
        graphOrNull?.close()
    }

    private fun openInternal(block: ComponentBuilderBlock?): Graph = Graph(
        application = this,
        parent = null,
        component = component,
        onCloseCallback = {
            lock.withLock { graphOrNull = null }
        },
        block = block
    ).also { graphOrNull = it }

}
