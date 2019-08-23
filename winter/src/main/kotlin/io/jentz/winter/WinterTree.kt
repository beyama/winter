package io.jentz.winter

import io.jentz.winter.WinterTree.State.Initialized
import io.jentz.winter.WinterTree.State.Uninitialized

/**
 * WinterTree acts as an holder for the application object graph and is a helper for
 * opening, closing and accessing subgraphs by paths of identifiers.
 *
 * Instances of [WinterTree] are usually not used directly but in injection adapters.
 *
 * This is inspired by [Toothpicks](https://github.com/stephanenicolas/toothpick)
 * openScope/closeScope mechanism, if you like that, you can simply use [GraphRegistry] instead of
 * the [Injection] abstraction.
 *
 * Example:
 * ```
 * // register the application component
 * Winter.component {
 *   // ... the component definition
 * }
 * val tree = WinterTree(Winter)
 *
 * // open the application graph
 * tree.open()
 * // or supply a builder block to extend the resulting graph
 * tree.open { constant<Application>(myApplication) }
 *
 * // the application graph can then be accessed by calling
 * tree.get()
 *
 * // to open a subgraph call
 * tree.open("subcomponent qualifier")
 *
 * // this graph can be accessed by calling
 * tree.get("subcomponent qualifier")
 *
 * // you can provide an optional identifier for the subgraph
 * tree.open("subcomponent qualifier", identifier = "other name")
 *
 * // then you can access the the subgraph by calling
 * tree.get("other name")
 *
 * // to open a subgraph of this call
 * tree.open("subcomponent qualifier", "sub-subcomponent qualifier")
 * // respectively
 * tree.open("other name", "sub-subcomponent qualifier")
 * ```
 */
open class WinterTree(private val application: WinterApplication) {

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

    private inline fun <T> fold(
        ifUninitialized: () -> T,
        ifInitialized: (Initialized) -> T
    ): T = synchronized(this) {
        when (val state = state) {
            is Uninitialized -> ifUninitialized()
            is Initialized -> ifInitialized(state)
        }
    }

    /**
     * Get a registered object graph by path.
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
     * Returns true if an entry under the given [path] exists otherwise false.
     */
    fun has(vararg path: Any): Boolean = fold({ false }) { it.getOrNull(path) != null }

    /**
     * Create and return an object graph by (sub-)component path without registering it.
     *
     * @param path The path of the (sub-)graph to initialize.
     * @param block An optional [ComponentBuilderBlock] that's passed to the (sub-)component
     *                     createGraph method.
     *
     * @return The created [Graph].
     *
     * @throws WinterException When application component is not set or path can not be resolved.
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
        application.createGraph(block)
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
     * Create a object graph by (sub-)component path and register it.
     * Opened object graphs will be children of each other in left to right order.
     *
     * @param path The path of the (sub-)graph to initialize.
     * @param identifier An optional identifier to store the subgraph under.
     * @param block An optional [ComponentBuilderBlock] that's passed to the (sub-)component
     *                     createGraph method.
     *
     * @return The newly created and registered graph.
     *
     * @throws WinterException When application component is not set or path can not be resolved.
     * @throws IllegalArgumentException When [path] is empty (root) but [identifier] is given.
     */
    fun open(
        vararg path: Any,
        identifier: Any? = null,
        block: ComponentBuilderBlock? = null
    ): Graph = fold({
        openApplicationGraph(path, identifier, block)
    }) { state ->
        openSubgraph(state, path, identifier, block)
    }

    private fun openApplicationGraph(
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
        return application.createGraph(block).also { state = Initialized(it) }
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
        
        graph.dispose()

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
