package io.jentz.winter

import io.jentz.winter.WinterTree.State.*

/**
 * WinterTree manages dependency graphs in a tree (directed acyclic graph).
 * Graphs a opened, accessed and closed by paths.
 *
 * Example:
 * ```
 * val tree = WinterTree()
 * tree.component = component {
 *   // ... the component definition
 * }
 * // open the root graph (initialize the trees component)
 * tree.open()
 * // or supply a builder block to extend the resulting graph
 * tree.open { constant<Application>(myApplication) }
 * // the root graph can then be accessed by calling
 * tree.get()
 * // to open a subcomponent call
 * tree.open("subcomponent name")
 * // this graph can be accessed by calling
 * tree.get("subcomponent name")
 * // or you can provide an optional identifier for the graph
 * tree.open("subcomponent name", identifier = "other name")
 * // then you can access the the graph by calling
 * tree.get("other name")
 * // to open a subcomponent of this call
 * tree.open("subcomponent name", "sub-subcomponent name")
 * // respectively
 * tree.open("other name", "sub-subcomponent name")
 * ```
 * For more examples and documentation see the standalone implementation [GraphRegistry].
 *
 * @see GraphRegistry
 */
open class WinterTree {

    private class Node(
        val parent: Node?,
        val name: Any,
        val graph: Graph
    ) {
        val children: MutableMap<Any, Node> = mutableMapOf()

        fun dispose() {
            disposeChildrenAndGraph()
            removeFromParent()
        }

        private fun disposeChildrenAndGraph() {
            children.values.forEach(Node::disposeChildrenAndGraph)
            graph.dispose()
        }

        private fun removeFromParent() {
            parent?.children?.remove(name)
        }

        fun getNodeOrNull(path: Array<out Any>, depth: Int = -1): Node? {
            if (path.isEmpty()) return this
            var node: Node = this
            var count = 0
            for (token in path) {
                if (depth > -1 && count == depth) break
                node = node.children[token] ?: return null
                count++
            }
            return node
        }
    }

    private sealed class State {
        object Uninitialized : State()
        class ComponentSet(val component: Component) : State()
        class Initialized(val component: Component, val root: Node) : State()
    }

    private var state: State = Uninitialized

    private inline fun <T> synchronizedState(fn: (State) -> T): T =
        synchronized(this) { fn(state) }

    private inline fun <T> foldInitialized(
        ifUninitialized: () -> T,
        ifInitialized: (Initialized) -> T
    ): T = synchronizedState {
        if (it is Initialized) ifInitialized(it) else ifUninitialized()
    }

    /**
     * The [component][Component] that is used to create the root dependency graph.
     *
     * Setting this will close all previously opened graphs.
     */
    var component: Component?
        get() = synchronizedState { state ->
            when (state) {
                is Uninitialized -> null
                is ComponentSet -> state.component
                is Initialized -> state.component
            }
        }
        set(value) {
            synchronized(this) {
                closeIfOpen()
                state = if (value != null) ComponentSet(value) else Uninitialized
            }
        }

    /**
     * Get a registered dependency graph by path.
     *
     * @param path The path of the graph.
     * @return The graph that is stored in [path].
     *
     * @throws WinterException When no graph is open or when no graph was found in [path].
     */
    fun get(vararg path: Any): Graph = foldInitialized<Graph>({
        throw WinterException("GraphRegistry.get called but there is no open graph.")
    }) { state ->
        state.root.getNodeOrNull(path)?.graph
            ?: throw WinterException("No graph in path `${pathToString(path)}` found.")
    }

    /**
     * Returns true if an entry under the given [path] exists otherwise false.
     */
    fun has(vararg path: Any): Boolean =
        foldInitialized({ false }) { it.root.getNodeOrNull(path) != null }

    /**
     * Create and return dependency graph by (sub-)component path without registering it.
     *
     * @param path The path of the (sub-)component to initialize.
     * @param builderBlock An optional [ComponentBuilderBlock] that's passed to the (sub-)component
     *                     init method.
     *
     * @return The created [Graph].
     *
     * @throws WinterException When application component is not set or path can not be resolved.
     */
    fun create(
        vararg path: Any,
        builderBlock: ComponentBuilderBlock? = null
    ): Graph = synchronizedState { state ->
        when (state) {
            is Uninitialized -> throw WinterException("No component set.")
            is ComponentSet -> {
                if (path.isNotEmpty()) {
                    throw WinterException(
                        "Cannot create `${pathToString(path)}` because root graph is not open."
                    )
                }
                state.component.init(builderBlock)
            }
            is Initialized -> {
                val parentNode = state.root.getNodeOrNull(path, path.lastIndex)
                    ?: throw WinterException(
                        "Cannot create `${pathToString(path)}` because " +
                                "`${pathToString(path, path.lastIndex)}` is not open."
                    )
                val qualifier = path.last()
                parentNode.graph.initSubcomponent(qualifier, builderBlock)
            }
        }
    }

    /**
     * Create a dependency graph by (sub-)component path and register it.
     * Opened components will be children of each other in left to right order.
     *
     * @param path The path of the (sub-)component to initialize.
     * @param identifier An optional identifier to store the sub dependency graph under.
     * @param builderBlock An optional [ComponentBuilderBlock] that's passed to the (sub-)component
     *                     init method.
     *
     * @return The newly created and registered graph.
     *
     * @throws WinterException When application component is not set or path can not be resolved.
     * @throws IllegalArgumentException When [path] is empty (root) but [identifier] is given.
     */
    fun open(
        vararg path: Any,
        identifier: Any? = null,
        builderBlock: ComponentBuilderBlock? = null
    ): Graph = synchronizedState { state ->
        when (state) {
            is Uninitialized -> throw WinterException("No component set.")
            is ComponentSet -> {
                if (path.isNotEmpty()) {
                    throw WinterException(
                        "Cannot open path `${pathToString(path)}` because root graph is not opened."
                    )
                }
                if (identifier != null) {
                    throw IllegalArgumentException(
                        "Argument `identifier` for root graph is not supported."
                    )
                }
                val rootGraph = state.component.init(builderBlock)

                this.state = Initialized(
                    state.component,
                    Node(null, Any(), rootGraph)
                )

                rootGraph
            }
            is Initialized -> {
                if (path.isEmpty()) {
                    throw WinterException("Cannot open root graph because it is already open.")
                }
                val parentNode = state.root.getNodeOrNull(path, path.lastIndex)
                    ?: throw WinterException(
                        "GraphRegistry.open can't open `${pathToString(path)}` because " +
                                "`${pathToString(path, path.lastIndex)}` is not open."
                    )

                val qualifier = path.last()
                val name = identifier ?: qualifier

                if (parentNode.children[name] != null) {
                    throw WinterException(
                        "Cannot open `${pathToString(path, path.lastIndex, name)}` because it " +
                                "is already open."
                    )
                }

                val graph = parentNode.graph.initSubcomponent(qualifier, builderBlock)
                parentNode.children[name] = Node(parentNode, name, graph)
                graph
            }
        }
    }

    /**
     * Remove and dispose the dependency graph and its children stored in [path].
     *
     * @param path The path of the graph to dispose.
     *
     * @throws WinterException When no graph was found in path.
     */
    fun close(vararg path: Any) {
        foldInitialized({
            throw WinterException("Cannot close because noting is open.")
        }) { state ->
            val node = state.root.getNodeOrNull(path) ?: throw WinterException(
                "Cannot close `${pathToString(path)}` because it doesn't exist."
            )
            disposeNode(state, node)
        }
    }

    /**
     * Remove and dispose the dependency graph and its children stored in [path] if it is open.
     *
     * @param path The path of the graph to dispose.
     *
     * @return true if given [path] was open otherwise false.
     */
    fun closeIfOpen(vararg path: Any): Boolean =
        foldInitialized({ false }) { state ->
            val node = state.root.getNodeOrNull(path) ?: return false
            disposeNode(state, node)
            true
        }

    private fun disposeNode(state: Initialized, node: Node) {
        node.dispose()
        if (state.root === node) {
            this.state = ComponentSet(state.component)
        }
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
