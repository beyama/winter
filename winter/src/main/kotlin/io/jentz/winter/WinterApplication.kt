package io.jentz.winter

import io.jentz.winter.WinterApplication.InjectionAdapter
import io.jentz.winter.delegate.DelegateNotifier
import io.jentz.winter.plugin.EMPTY_PLUGINS
import io.jentz.winter.plugin.Plugins

/**
 * Holds plugins, the application [Component], the application [Graph] and offers an
 * abstraction to open, get and close dependency graphs for a class that cannot make use of
 * constructor injection using an [InjectionAdapter] system.
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
 * Example using the SimpleAndroidInjectionAdapter which is part of the winter-android module:
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
 *     // create root graph
 *     Winter.createGraph(this)
 *   }
 * }
 *
 * class MyActivity : Activity() {
 *   private val injector = Injector()
 *   private val viewModel: RepoListViewModel by injector.instance()
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     Winter.createGraphAndInject(this, injector)
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     Winter.disposeGraph(this)
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

    /**
     * The application component.
     */
    var component: Component? = null
        set(value) {
            synchronized(tree) {
                if (tree.isOpen() && field !== value) {
                    throw WinterException(
                        "Cannot set component because application graph is already open"
                    )
                }
                field = value
            }
        }

    /**
     * Get instance of [Tree].
     */
    @Suppress("LeakingThis")
    val tree = Tree(this)

    var injectionAdapter: InjectionAdapter? = null
        set(value) {
            synchronized(tree) {
                if (tree.isOpen() && field !== value) {
                    throw WinterException(
                        "Cannot set injection adapter because application graph is already open"
                    )
                }
                field = value
            }
        }

    private val requireInjectionAdapter: InjectionAdapter
        get() = checkNotNull(injectionAdapter) {
            "Application injection adapter is not set."
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
     * Used internally for injected properties.
     */
    internal val delegateNotifier = DelegateNotifier()

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
     * Open and return dependency graph for [instance].
     *
     * @param instance The instance for which a graph should be opened.
     * @param block An optional builder block to derive the component with.
     * @return The newly opened graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun openGraph(instance: Any, block: ComponentBuilderBlock? = null): Graph =
        requireInjectionAdapter.open(instance, block) ?: throw WinterException(
            "Could not open graph for `$instance` type is not supported."
        )

    /**
     * Open and return dependency graph for [instance] and inject all members into instance.
     *
     * @param instance The instance to open a graph for and to inject into.
     * @param block An optional builder block to derive the component with.
     * @return The created dependency graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun openGraphAndInject(
        instance: Any,
        block: ComponentBuilderBlock? = null
    ): Graph = openGraph(instance, block).also { graph ->
        graph.inject(instance)
    }

    /**
     * Get dependency graph for [instance].
     *
     * @param instance The instance to retrieve the dependency graph for.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     *
     */
    fun getGraph(instance: Any): Graph = requireInjectionAdapter.get(instance)
        ?: throw WinterException("No graph found for instance `$instance`.")

    /**
     * Get or open dependency graph for [instance].
     *
     * @param instance The instance for which a graph should be retrieved or opened.
     * @param block An optional builder block to derive the component with.
     * @return The graph for [instance].
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun getOrOpenGraph(
        instance: Any,
        block: ComponentBuilderBlock?
    ): Graph = requireInjectionAdapter.get(instance) ?: openGraph(instance, block)

    /**
     * Check if graph for [instance] is open.
     *
     * @param instance The instance to check for a open graph for.
     * @return True if graph for instance is open otherwise false.
     */
    fun isGraphOpen(instance: Any): Boolean = requireInjectionAdapter.get(instance) != null

    /**
     * Dispose the dependency graph of the given [instance].
     *
     * @param instance The instance to close the graph for.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun closeGraph(instance: Any) {
        requireInjectionAdapter.close(instance)
    }

    /**
     * Close dependency graph for [instance] if open.
     *
     * @param instance The instance to close the graph for.
     */
    fun closeGraphIfOpen(instance: Any) {
        if (isGraphOpen(instance)) closeGraph(instance)
    }

    /**
     * Inject into [instance] by using the dependency graph of the [instance].
     *
     * Sugar for calling `getGraph(instance).inject(instance)`.
     *
     * @param instance The instance to retrieve the dependency graph for and inject dependencies
     *                 into.
     * @throws [io.jentz.winter.WinterException] If given [instance] type is not supported.
     */
    fun inject(instance: Any) {
        getGraph(instance).inject(instance)
    }

    /**
     * Adapter interface to provide application specific graph creation and retrieval strategy.
     */
    interface InjectionAdapter {

        /**
         * Get dependency graph for [instance].
         *
         * @param instance The instance to get the graph for.
         * @return The graph for [instance] or null when no open graph for instance was found.
         */
        fun get(instance: Any): Graph?

        /**
         * Open dependency graph for [instance].
         *
         * @param instance The instance to open a dependency graph for.
         * @param block An optional builder block to derive the component with.
         * @return The newly opened graph or null if [instance] type is not supported.
         */
        fun open(instance: Any, block: ComponentBuilderBlock?): Graph?

        /**
         * Close the dependency graph of the given [instance].
         *
         * @param instance The instance to close the graph for.
         * @throws [io.jentz.winter.WinterException] if no graph for this [instance] type exists.
         */
        fun close(instance: Any)
    }

}
