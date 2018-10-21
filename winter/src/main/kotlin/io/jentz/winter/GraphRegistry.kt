package io.jentz.winter

/**
 * The graph registry creates and holds dependency graphs in a tree (directed acyclic graph).
 *
 * For example consider the following application component of a basic Android application:
 *
 * ```
 * GraphRegistry.applicationComponent = component { // the application component
 *   // A presentation subcomponent that survives orientation changes
 *   subcomponent("presentation") {
 *     // The activity subcomponent that gets recreated with every device rotation
 *     subcomponent("activity") {
 *     }
 *   }
 * }
 * ```
 *
 * Create the application dependency graph on application start:
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
 *     super.onCreate()
 *
 *     GraphRegistry.open() {
 *       constant<Application> { this@MyApplication }
 *       constant<Context> { this@MyApplication }
 *     }
 *   }
 * }
 * ```
 *
 * Create the presenter and activity dependency graph by its path (of qualifiers):
 *
 * ```
 * class MyActivity : Activity() {
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     // open presentation graph if not already open
 *     if (!GraphRegistry.has("presentation")) GraphRegistry.open("presentation")
 *     // open activity graph
 *     GraphRegistry.open("presentation", "activity") {
 *       constant<Activity>(theActivityInstance)
 *     }
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     // if we are leaving the scope of the activity then we close "presentation"
 *     if (isFinishing) {
 *       GraphRegistry.close("presentation")
 *     // and if this is just recreating then we just close "activity"
 *     } else {
 *       GraphRegistry.close("presentation", "activity")
 *     }
 *     super.onDestroy()
 *   }
 *
 * }
 * ```
 *
 * If you need multiple instances of the same subcomponent you can pass an `identifier` parameter
 * to the open method to register the graph instance under a different `identifier` than its
 * component qualifier:
 *
 * ```
 * GraphRegistry.open("presentation", "activity", identifier: theActivityInstance) {
 *   constant<Activity>(theActivityInstance)
 * }
 * ```
 *
 * Close the activity graph:
 *
 * ```
 * GraphRegistry.close("presentation", "activity")
 * ```
 *
 * Close the activity graph that was created with an identifier:
 *
 * ```
 * GraphRegistry.close("presentation", theActivityInstance)
 * ```
 *
 * [GraphRegistry.close] will remove and dispose all child dependency graphs from the registry.
 * So in our example above the call:
 *
 * ```
 * GraphRegistry.close("presentation")
 * ```
 *
 * will also close all activity dependency graphs.
 *
 * To get an instance of a dependency graph use [get][GraphRegistry.get]:
 * ```
 * GraphRegistry.get()               // Get the root dependency graph
 * GraphRegistry.get("presentation") // Get the presentation dependency graph
 * ```
 */
object GraphRegistry {

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

        class ApplicationComponentSet(
            val applicationComponent: Component
        ) : State()

        class Initialized(
            val applicationComponent: Component,
            val root: Node
        ) : State()
    }

    private var state: State = State.Uninitialized

    private inline fun <T> synchronizedState(fn: (State) -> T): T =
        synchronized(this) { fn(state) }

    private inline fun <T> foldInitialized(
        ifNot: () -> T,
        ifInitialized: (State.Initialized) -> T
    ): T = synchronizedState {
        if (it is State.Initialized) ifInitialized(it) else ifNot()
    }

    /**
     * The application [Component] that's used to create the root dependency graph.
     *
     * Setting this will close all previously opened graphs.
     */
    @JvmStatic
    var applicationComponent: Component?
        get() = synchronizedState { state ->
            when (state) {
                is State.Uninitialized -> null
                is State.ApplicationComponentSet -> state.applicationComponent
                is State.Initialized -> state.applicationComponent
            }
        }
        set(value) {
            synchronized(this) {
                if (has()) close()
                this.state = if (value == null) {
                    State.Uninitialized
                } else {
                    State.ApplicationComponentSet(value)
                }
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
    @JvmStatic
    fun get(vararg path: Any): Graph = foldInitialized<Graph>({
        throw WinterException("GraphRegistry.get called but there is no open graph.")
    }) {
        it.root.getNodeOrNull(path)?.graph
            ?: throw WinterException("No graph in path `${pathToString(path)}` found.")
    }

    /**
     * Returns true if an entry under the given [path] exists otherwise false.
     */
    @JvmStatic
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
    @JvmStatic
    fun create(
        vararg path: Any,
        builderBlock: ComponentBuilderBlock? = null
    ): Graph = synchronizedState { state ->
        when (state) {
            is State.Uninitialized -> {
                throw WinterException(
                    "GraphRegistry.create called but there is no application component set."
                )
            }
            is State.ApplicationComponentSet -> {
                if (path.isNotEmpty()) {
                    throw WinterException(
                        "GraphRegistry.create with path `${pathToString(path)}` called but root " +
                                "graph isn't open."
                    )
                }
                state.applicationComponent.init(builderBlock)
            }
            is State.Initialized -> {
                val parentNode = state.root.getNodeOrNull(path, path.lastIndex)
                    ?: throw WinterException(
                        "GraphRegistry.create can't open `${pathToString(path)}` because " +
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
    @JvmStatic
    fun open(
        vararg path: Any,
        identifier: Any? = null,
        builderBlock: ComponentBuilderBlock? = null
    ): Graph = synchronizedState { state ->
        when (state) {
            is State.Uninitialized -> {
                throw WinterException(
                    "GraphRegistry.open called but there is no application component set."
                )
            }
            is State.ApplicationComponentSet -> {
                if (path.isNotEmpty()) {
                    throw WinterException(
                        "GraphRegistry.open with path `${pathToString(path)}` called but root " +
                                "graph isn't initialized."
                    )
                }
                if (identifier != null) {
                    throw IllegalArgumentException(
                        "GraphRegistry.open must not be called with identifier for root graph."
                    )
                }
                val rootGraph = state.applicationComponent.init(builderBlock)

                this.state = State.Initialized(
                    state.applicationComponent,
                    Node(null, Any(), rootGraph)
                )

                rootGraph
            }
            is State.Initialized -> {
                if (path.isEmpty()) {
                    throw WinterException(
                        "GraphRegistry.open can't open root graph because it is already open."
                    )
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
                        "GraphRegistry.open can't open " +
                                "'${pathToString(path, path.lastIndex, name)}' because it " +
                                "already exists."
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
    @JvmStatic
    fun close(vararg path: Any) {
        foldInitialized({
            throw WinterException("GraphRegistry.close called but no graph is open.")
        }) {
            val node = it.root.getNodeOrNull(path)
                ?: throw WinterException(
                    "GraphRegistry.close can't close `${pathToString(path)}` because it doesn't " +
                            "exist."
                )
            node.dispose()
            if (it.root === node) {
                this.state = State.ApplicationComponentSet(it.applicationComponent)
            }
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
