package io.jentz.winter

/**
 * The graph registry creates and holds dependency graphs in a tree (directed acyclic graph).
 *
 * For example consider the following application component of a basic Android application:
 *
 * ```
 * GraphRegistry.applicationComponent = component { // the application component
 *   subcomponent("presentation") {                 // A presentation subcomponent that survives orientation changes
 *     subcomponent("activity") {}                  // The activity subcomponent that gets recreated with every device rotation
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
 *     GraphRegistry.open() { constant<Application> { this@MyApplication }
 *   }
 * }
 * ```
 *
 * Create the presenter dependency graph by its path (of qualifiers) if it doesn't already exist:
 *
 * ```
 * if (!GraphRegistry.has("presentation") {
 *   GraphRegistry.open("presentation")
 * }
 * ```
 *
 * Create the activity dependency graph by its path (of qualifiers):
 *
 * ```
 * GraphRegistry.open("presentation", "activity") { constant<Activity>(theActivityInstance) }
 * ```
 *
 * If you need multiple instances of the same subcomponent you can pass an `identifier` parameter to the open method
 * to register the graph instance under a different `identifier` than its component qualifier:
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

    private class Node(val parent: Node?, val name: Any, val graph: Graph) {
        val children = mutableMapOf<Any, Node>()
    }

    /**
     * The application [Component] that's used to create the root dependency graph.
     */
    @JvmStatic
    var applicationComponent: Component? = null

    private var rootNode: Node? = null

    private val root get() = rootNode ?: throw EntryNotFoundException("Root dependency graph isn't initialized.")

    /**
     * Get a registered dependency graph by path.
     */
    @JvmStatic
    fun get(vararg path: Any): Graph {
        val node = getNode(path)
                ?: throw EntryNotFoundException("Dependency graph in path '${pathToString(path)}' doesn't exist.")
        return node.graph
    }

    /**
     * Returns true if an entry under the given path exists otherwise false.
     */
    @JvmStatic
    fun has(vararg path: Any): Boolean {
        rootNode ?: return false
        return getNode(path) != null
    }

    /**
     * Create and return dependency graph by (sub-)component path.
     */
    @JvmStatic
    fun create(vararg path: Any, builderBlock: ComponentBuilderBlock? = null): Graph {
        if (path.isEmpty()) {
            val component = applicationComponent
                    ?: throw WinterException("Can't create root dependency graph without application component.")
            return component.init(builderBlock)
        }
        val parentPath = path.copyOfRange(0, path.lastIndex)
        val qualifier = path.last()
        return create(parentPath, qualifier, builderBlock)
    }

    /**
     * Create a dependency graph by (sub-)component path and register it.
     * Opened components will be children of each other in left to right order.
     *
     * @param path The path of the (sub-)component to initialize.
     * @param identifier An optional identifier to store the sub dependency graph under.
     * @param builderBlock An optional [ComponentBuilderBlock] that's passed to the (sub-)component init method.
     */
    @JvmStatic
    fun open(vararg path: Any, identifier: Any? = null, builderBlock: ComponentBuilderBlock? = null): Graph {
        synchronized(this) {
            if (path.isEmpty()) {
                if (identifier != null) throw WinterException("Identifier for root dependency graph is not allowed.")
                if (has()) throw WinterException("Root dependency graph is already open.")
                return create(builderBlock = builderBlock).also { registerRoot(it) }
            }

            val parentPath = path.copyOfRange(0, path.lastIndex)
            val qualifier = path.last()
            val name = identifier ?: qualifier
            val target = if (identifier == null) path else arrayOf(*parentPath, identifier)

            if (has(*target)) throw WinterException("Graph under '${pathToString(target)}' already exists.")

            return create(parentPath, qualifier, builderBlock).also { register(parentPath, name, it) }
        }
    }

    /**
     * Remove and dispose the dependency graph and its children stored in [path].
     */
    @JvmStatic
    fun close(vararg path: Any) {
        synchronized(this) {
            val root = rootNode ?: throw WinterException("Close called but nothing is open.")
            val node = path.fold(root) { node, qualifier ->
                node.children[qualifier]
                        ?: throw WinterException("Subcomponent with path '${pathToString(path)}' is not initialized.")
            }
            disposeNode(node)
            node.parent?.children?.remove(node.name)
            if (node === rootNode) rootNode = null
        }
    }

    private fun disposeNode(node: Node) {
        node.children.values.forEach { disposeNode(it) }
        node.graph.dispose()
    }

    private fun getNode(path: Array<out Any>): Node? {
        if (path.isEmpty()) return root
        return path.fold(root) { node, token -> node.children[token] ?: return null }
    }

    private fun create(parentPath: Array<out Any>, qualifier: Any, builderBlock: ComponentBuilderBlock?): Graph {
        val parent = get(*parentPath)
        return parent.initSubcomponent(qualifier, builderBlock)
    }

    private fun registerRoot(graph: Graph) {
        rootNode = Node(null, Any(), graph)
    }

    private fun register(parentPath: Array<out Any>, name: Any, graph: Graph) {
        val parent = getNode(parentPath)
                ?: throw EntryNotFoundException("Parent in path '${pathToString(parentPath)}' doesn't exist.")
        parent.children[name] = Node(parent, name, graph)
    }

    private fun pathToString(path: Array<out Any>) = path.joinToString(".")

}