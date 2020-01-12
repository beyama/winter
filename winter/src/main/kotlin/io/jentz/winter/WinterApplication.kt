package io.jentz.winter

import io.jentz.winter.WinterApplication.InjectionAdapter
import io.jentz.winter.inject.ApplicationScope
import io.jentz.winter.plugin.Plugins

/**
 * Holds plugins, the application [Component], the application [Graph] and offers an abstraction to
 * inject dependencies into classes that cannot make use of constructor injection using an
 * [InjectionAdapter] system.
 *
 * The [InjectionAdapter] backed abstraction takes the burden off of the using class to know how
 * exactly a graph or parent graph is constructed and stored.
 *
 * An application specific graph creation and retrieval strategy can be provided by setting a
 * custom [InjectionAdapter].
 *
 * ```
 * Winter.adapter = MyCustomAdapter()
 * ```
 *
 * To use Winter in a library it is recommended to create a library specific object
 * from [WinterApplication] for use in applications it is recommended to use the [Winter] object.
 *
 * Example, using the SimpleAndroidInjectionAdapter which is part of the winter-androidx module:
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
 *     super.onCreate()
 *
 *     // register component
 *     Winter.component {
 *       singleton<GitHubApi> { GitHubApiImpl() }
 *
 *       singleton { RepoListViewModel(instance()) }
 *
 *       subcomponent("activity") {
 *          singleton { Glide.with(instance<Activity>()) }
 *       }
 *     }
 *
 *     // register adapter
 *     Winter.useSimpleAndroidAdapter()
 *     // open application graph
 *     Winter.inject(this)
 *   }
 * }
 *
 * class MyActivity : Activity(), WinterAware {
 *   private val viewModel: RepoListViewModel by inject()
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     Winter.inject(this)
 *     super.onCreate(savedInstanceState)
 *   }
 *
 * }
 *
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
        qualifier: Any = ApplicationScope::class,
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
            synchronized(this) {
                if (graphOrNull != null) {
                    throw WinterException(
                        "Cannot set component because application graph is already open."
                    )
                }
                field = value
            }
        }

    /**
     * The application injection adapter.
     */
    var injectionAdapter: InjectionAdapter? = null
        set(value) {
            synchronized(this) {
                if (graphOrNull != null) {
                    throw WinterException(
                        "Cannot set injection adapter because application graph is already open."
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
    fun component(qualifier: Any = ApplicationScope::class, block: ComponentBuilderBlock) {
        this.component = io.jentz.winter.component(qualifier, block)
    }

    /**
     * Open the application component.
     *
     * @param block Optional builder block to derive the application component.
     *
     * @return The newly opened application graph.
     */
    fun openGraph(block: ComponentBuilderBlock? = null): Graph = synchronized(this) {
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
    fun getOrOpenGraph(block: ComponentBuilderBlock? = null): Graph = synchronized(this) {
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
            synchronized(this) { graphOrNull = null }
        },
        block = block
    ).also { graphOrNull = it }

    /**
     * Inject dependencies into [instance] by using the dependency graph returned from
     * [InjectionAdapter.get] called with [instance].
     *
     * @param instance The instance to retrieve the dependency graph for and inject dependencies
     *                 into.
     * @throws [io.jentz.winter.WinterException] If given [instance] type is not supported.
     */
    fun inject(instance: Any) {
        val adapter = injectionAdapter ?: throw WinterException(
            "No injection adapter configured."
        )
        val graph = adapter.get(instance) ?: throw WinterException(
            "No graph found for instance `$instance`."
        )
        graph.inject(instance)
    }

    /**
     * Adapter interface to provide application specific graph creation and retrieval strategy.
     */
    interface InjectionAdapter {

        /**
         * Get dependency graph for [instance].
         *
         * @param instance The instance to get the graph for.
         * @return The graph for [instance] or null when instance type is not supported.
         */
        fun get(instance: Any): Graph?

    }

}
