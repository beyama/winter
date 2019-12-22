package io.jentz.winter

import io.jentz.winter.WinterApplication.State.Initialized
import io.jentz.winter.WinterApplication.State.Uninitialized
import io.jentz.winter.plugin.EMPTY_PLUGINS
import io.jentz.winter.plugin.Plugins

/**
 * [WinterApplication] base class that holds Winter plugins and may be configured with the
 * application [Component].
 *
 * It acts as an holder for the application object graph and offers utility methods for
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
 * Winter.open()
 * // or supply a builder block to extend the resulting graph
 * Winter.open { constant<Application>(myApplication) }
 *
 * // the application graph can then be accessed by calling
 * Winter.get()
 *
 * // to open a subgraph call
 * Winter.open("subcomponent qualifier")
 *
 * // this graph can be accessed by calling
 * Winter.get("subcomponent qualifier")
 *
 * // you can provide an optional identifier for the subgraph
 * Winter.open("subcomponent qualifier", identifier = "other name")
 *
 * // then you can access the the subgraph by calling
 * Winter.get("other name")
 *
 * // to open a subgraph of this call
 * Winter.open("subcomponent qualifier", "sub-subcomponent qualifier")
 * // respectively
 * Winter.open("other name", "sub-subcomponent qualifier")
 * ```
 *
 * Here an Android example where we create a presentation graph that survives configuration changes
 * and an Activity graph that gets recreated every time.
 *
 * It is recommended to hide such details in a [WinterInjection.Adapter] and use the [Injection]
 * abstraction.
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
 *     Winter.open() {
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
 *     if (!Winter.has(javaClass)) Winter.open("presentation", identifier = javaClass)
 *
 *     // Open the activity graph.
 *     // Here the same but we use the instance as identifier for the activity graph.
 *     Winter.open(javaClass, "activity", identifier = this) {
 *       constant<Context>(this@MyActivity)
 *       constant<Activity>(this@MyActivity)
 *     }
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     // If we are leaving the scope of the activity then we close the "presentation" graph.
 *     if (isFinishing) {
 *       Winter.close(javaClass)
 *     // And if this is just recreating then we just close the "activity" graph.
 *     } else {
 *       Winter.close(javaClass, this)
 *     }
 *     super.onDestroy()
 *   }
 *
 * }
 * ```
 */
open class WinterApplication() {

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
        qualifier: Any = APPLICATION_COMPONENT_QUALIFIER,
        block: ComponentBuilderBlock
    ) : this() {
        component(qualifier, block)
    }

    private sealed class State {
        class Uninitialized(val component: Component) : State()

        class Initialized(
            val component: Component,
            val root: Graph
        ) : State() {

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

    private var state: State = Uninitialized(emptyComponent())

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
     * The application component.
     */
    var component
        get() = fold({ it.component }, { it.component })
        set(value) {
            fold({
                state = Uninitialized(value)
            }, {
                throw WinterException(
                    "Cannot set component because application graph is already open"
                )
            })
        }

    /**
     * The plugins registered on the application.
     */
    var plugins: Plugins = EMPTY_PLUGINS

    /**
     * If this is set to true, Winter will check for cyclic dependencies and throws an error if it
     * encounters one. Without this check you will run in a StackOverflowError when you accidentally
     * declared a cyclic dependency which may be hard to track down.
     *
     * Cyclic dependency checks are a bit more expensive but usually worth it in debug or test
     * builds.
     *
     */
    var checkForCyclicDependencies: Boolean = false

    /**
     * Sets the application component by supplying an optional qualifier and a component builder
     * block.
     *
     * @param qualifier The qualifier for the new component.
     * @param block The component builder block.
     */
    fun component(qualifier: Any = APPLICATION_COMPONENT_QUALIFIER, block: ComponentBuilderBlock) {
        this.component = io.jentz.winter.component(qualifier, block)
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
     * @throws WinterExcepWhen application component is not set or path can not be resolved.
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

        it.component.createGraph(this, block)
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
        openApplicationGraph(it.component, path, identifier, block)
    }) { state ->
        openSubgraph(state, path, identifier, block)
    }

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

        return component.createGraph(this, block).also {
            state = Initialized(component, it)
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

        graph.dispose()

        if (path.isEmpty()) {
            this.state = Uninitialized(state.component)
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
