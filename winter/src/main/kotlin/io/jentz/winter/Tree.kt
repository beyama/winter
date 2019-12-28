package io.jentz.winter

import io.jentz.winter.Tree.State.Initialized
import io.jentz.winter.Tree.State.Uninitialized

/**
 * Holder for the application object graph and offers utility methods for
 * opening, closing and accessing subgraphs by paths of identifiers.
 *
 * Example:
 * ```
 * // register the application component
 * Winter.component {
 *   // ... the component definition
 * }
 *
 * // open the application graph
 * Winter.tree.open()
 * // or supply a builder block to extend the resulting graph
 * Winter.tree.open { constant<Application>(myApplication) }
 *
 * // the application graph can then be accessed by calling
 * Winter.tree.get()
 *
 * // to open a subgraph call
 * Winter.tree.open("subcomponent qualifier")
 *
 * // this graph can be accessed by calling
 * Winter.tree.get("subcomponent qualifier")
 *
 * // you can provide an optional identifier for the subgraph
 * Winter.tree.open("subcomponent qualifier", identifier = "other name")
 *
 * // then you can access the the subgraph by calling
 * Winter.tree.get("other name")
 *
 * // to open a subgraph of this call
 * Winter.tree.open("subcomponent qualifier", "sub-subcomponent qualifier")
 * // respectively
 * Winter.tree.open("other name", "sub-subcomponent qualifier")
 * ```
 *
 * Here an Android example where we create a presentation graph that survives configuration changes
 * and an Activity graph that gets recreated every time.
 *
 * It is recommended to hide such details in a [WinterApplication.InjectionAdapter] and use the
 * adapter based [WinterApplication] methods
 *
 * Create the application object graph on application start:
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
 *     super.onCreate()
 *
 *     // Register the application component
 *     Winter.component {
 *       // A presentation subcomponent that survives orientation changes
 *       subcomponent("presentation") {
 *         // The activity subcomponent that gets recreated with every device rotation
 *         subcomponent("activity") {
 *         }
 *       }
 *     }
 *
 *     // open the application graph
 *     Winter.tree.open() {
 *       constant<Application> { this@MyApplication }
 *       constant<Context> { this@MyApplication }
 *     }
 *   }
 * }
 * ```
 *
 * Create the presenter and activity object subgraphs by their paths (of qualifiers):
 *
 * ```
 * class MyActivity : Activity() {
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     // Open the presentation graph if not already open.
 *     // Since we could have multiple activity instances at the same time we use the Activity class
 *     // as an identifier for the presentation graph.
 *     Winter.tree.getOrOpen("presentation", identifier = javaClass)
 *
 *     // Open the activity graph.
 *     // Here the same but we use the instance as identifier for the activity graph.
 *     Winter.tree.open(javaClass, "activity", identifier = this) {
 *       constant<Context>(this@MyActivity)
 *       constant<Activity>(this@MyActivity)
 *     }
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     // If we are leaving the scope of the activity then we close the "presentation" graph.
 *     if (isFinishing) {
 *       Winter.tree.close(javaClass)
 *     // And if this is just recreating then we just close the "activity" graph.
 *     } else {
 *       Winter.tree.close(javaClass, this)
 *     }
 *     super.onDestroy()
 *   }
 *
 * }
 * ```
 */
class Tree(
    private val application: WinterApplication
) {

    private sealed class State {
        object Uninitialized : State()

        class Initialized(val root: Graph) : State() {

            fun getOrNull(path: Array<out Any>, depth: Int = -1): Graph? {
                if (path.isEmpty()) return root

                var graph: Graph = root

                path.forEachIndexed { index, token ->
                    if (depth > -1 && index == depth) return graph
                    graph = graph.instanceOrNull(token) ?: return null
                }
                return graph
            }

        }
    }

    private var state: State = Uninitialized

    private val requireComponent: Component
        get() = checkNotNull(application.component) { "Application component is not set." }

    private inline fun <T> fold(
        ifUninitialized: (Uninitialized) -> T,
        ifInitialized: (Initialized) -> T
    ): T = synchronized(this) {
        when (val state = state) {
            is Uninitialized -> ifUninitialized(state)
            is Initialized -> ifInitialized(state)
        }
    }

    /**
     * Get a dependency graph by path.
     *
     * @param path The path of the graph.
     * @return The graph that is stored in [path].
     *
     * @throws WinterException When no graph is open or when no graph was found in [path].
     */
    fun get(vararg path: Any): Graph = fold<Graph>({
        throw WinterException("No object graph opened.")
    }) {
        it.getOrNull(path)
            ?: throw WinterException("No object graph in path `${pathToString(path)}` found.")
    }

    /**
     * Get a dependency graph by path.
     *
     * @param path The path of the graph.
     * @return The graph that is stored in [path] or null if not graph in path is open.
     */
    fun getOrNull(vararg path: Any): Graph? = fold({ null }, { it.getOrNull(path) })

    /**
     * Returns true if an entry under the given [path] exists otherwise false.
     */
    fun isOpen(vararg path: Any): Boolean = fold({ false }) { it.getOrNull(path) != null }

    /**
     * Create and return a dependency graph by (sub-)component path without registering it.
     *
     * @param path The path of the (sub-)graph to create.
     * @param block An optional [ComponentBuilderBlock] that's passed to the (sub-)component
     *              createGraph method.
     *
     * @return The created [Graph].
     *
     * @throws WinterException when application component is not set or path can not be resolved.
     */
    fun create(
        vararg path: Any,
        block: ComponentBuilderBlock? = null
    ): Graph = fold({
        if (path.isNotEmpty()) {
            throw WinterException(
                "Cannot create `${pathToString(path)}` because application graph is not open."
            )
        }

        requireComponent.createGraph(application, block)
    }) { state ->
        val parentGraph = state.getOrNull(path, path.lastIndex)
            ?: throw WinterException(
                "Cannot create `${pathToString(path)}` because " +
                        "`${pathToString(path, path.lastIndex)}` is not open."
            )
        val qualifier = path.last()
        parentGraph.openSubgraph(qualifier, block = block)
    }

    /**
     * Open a dependency graph by (sub-)component path.
     * Opened object graphs will be children of each other in left to right order.
     *
     * @param path The path of the (sub-)graph to open.
     * @param identifier An optional identifier to store the subgraph under.
     * @param block An optional [ComponentBuilderBlock] that is passed to the (sub-)component
     *              createGraph method.
     *
     * @return The opened dependency graph.
     *
     * @throws WinterException When application component is not set or path can not be resolved.
     * @throws IllegalArgumentException When [path] is empty (root) but [identifier] is given.
     */
    fun open(
        vararg path: Any,
        identifier: Any? = null,
        block: ComponentBuilderBlock? = null
    ): Graph = fold({
        openApplicationGraph(requireComponent, path, identifier, block)
    }) { state ->
        openSubgraph(state, path, identifier, block)
    }

    /**
     * Get or open a dependency graph by (sub-)component path.
     * Opened object graphs will be children of each other in left to right order.
     *
     * @param path The path of the (sub-)graph to get or open.
     * @param identifier An optional identifier to store the subgraph under.
     * @param block An optional [ComponentBuilderBlock] that is passed to the (sub-)component
     *              createGraph method.
     *
     * @return The dependency graph.
     *
     * @throws WinterException When application component is not set or path can not be resolved.
     * @throws IllegalArgumentException When [path] is empty (root) but [identifier] is given.
     */
    fun getOrOpen(
        vararg path: Any,
        identifier: Any? = null,
        block: ComponentBuilderBlock? = null
    ): Graph = fold({
        openApplicationGraph(requireComponent, path, identifier, block)
    }, { state ->
        if (path.isEmpty()) {
            return@fold state.root
        }

        val qualifier = path.last()
        val name = identifier ?: qualifier

        state
            .getOrNull(path, path.lastIndex)
            ?.instanceOrNull<Graph>(name)
            ?.let { return@fold it }

        openSubgraph(state, path, identifier, block)
    })

    private fun openApplicationGraph(
        component: Component,
        path: Array<out Any>,
        identifier: Any?,
        block: ComponentBuilderBlock?
    ): Graph {
        if (path.isNotEmpty()) {
            throw WinterException(
                "Cannot open path `${pathToString(path)}` because application graph is not opened."
            )
        }
        if (identifier != null) {
            throw IllegalArgumentException(
                "Argument `identifier` for application graph is not supported."
            )
        }

        return Graph(
            application = application,
            parent = null,
            component = component,
            onCloseCallback = { close() },
            block = block
        ).also {
            state = Initialized(it)
        }
    }

    private fun openSubgraph(
        state: Initialized,
        path: Array<out Any>,
        identifier: Any?,
        block: ComponentBuilderBlock?
    ): Graph {
        if (path.isEmpty()) {
            throw WinterException("Cannot open application graph because it is already open.")
        }

        val parentGraph = state.getOrNull(path, path.lastIndex)
            ?: throw WinterException(
                "Can't open `${pathToString(path)}` because " +
                        "`${pathToString(path, path.lastIndex)}` is not open."
            )

        val qualifier = path.last()
        val name = identifier ?: qualifier

        try {
            return parentGraph.openSubgraph(qualifier, name, block)
        } catch (e: WinterException) {
            throw WinterException("Cannot open `${pathToString(path, path.lastIndex, name)}`.", e)
        }
    }

    /**
     * Remove and dispose the object graph and its subgraphs stored in [path].
     *
     * @param path The path of the graph to dispose.
     *
     * @throws WinterException When no graph was found in path.
     */
    fun close(vararg path: Any) {
        fold({
            throw WinterException("Cannot close because noting is open.")
        }) { state ->
            if (!close(state, path)) {
                throw WinterException(
                    "Cannot close `${pathToString(path)}` because it doesn't exist."
                )
            }
        }
    }

    /**
     * Remove and dispose the object graph and its subgraphs stored in [path] if it is open.
     *
     * @param path The path of the graph to dispose.
     *
     * @return true if given [path] was open otherwise false.
     */
    fun closeIfOpen(vararg path: Any): Boolean =
        fold({ false }) { state -> close(state, path) }

    private fun close(state: Initialized, path: Array<out Any>): Boolean {
        val graph = state.getOrNull(path) ?: return false

        graph.close()

        if (path.isEmpty()) {
            this.state = Uninitialized
        }
        return true
    }

    private fun pathToString(
        path: Array<out Any>,
        depth: Int = -1,
        identifier: Any? = null
    ): String {
        val buffer = StringBuffer()
        path.joinTo(
            buffer = buffer,
            separator = ".",
            limit = depth,
            truncated = "",
            postfix = identifier?.toString() ?: ""
        )
        if (depth > 0 && identifier == null) {
            buffer.deleteCharAt(buffer.lastIndex)
        }
        return buffer.toString()
    }

}
