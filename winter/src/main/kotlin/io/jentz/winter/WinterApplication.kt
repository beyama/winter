package io.jentz.winter

import io.jentz.winter.adapter.ApplicationGraphOnlyInjectionAdapter
import io.jentz.winter.plugin.EMPTY_PLUGINS
import io.jentz.winter.plugin.Plugins

/**
 * [WinterApplication] base class that holds Winter plugins and may be configured with the
 * application [Component].
 */
/**
 * Abstraction to create, get and dispose a dependency graph from a class that can't make use of
 * constructor injection. This takes the burden off of the class to know how exactly a graph
 * or parent graph is stored and how to create and store a new graph.
 *
 * An application specific graph creation and retrieval strategy can be provided by setting a
 * custom [InjectionAdapter].
 *
 * ```
 * Injection.adapter = MyCustomAdapter()
 * ```
 *
 * To use this abstraction in a library it is recommended to create a library specific object
 * from [WinterApplication].
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
 *     Injection.useSimpleAndroidAdapter()
 *     // create root graph
 *     Injection.createGraph(this)
 *   }
 * }
 *
 * class MyActivity : Activity() {
 *   private val injector = Injector()
 *   private val viewModel: RepoListViewModel by injector.instance()
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     Injection.createGraphAndInject(this, injector)
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     Injection.disposeGraph(this)
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
    var component: Component = emptyComponent()
        set(value) {
            synchronized(tree) {
                if (tree.isOpen()) {
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

    var injectionAdapter: InjectionAdapter = ApplicationGraphOnlyInjectionAdapter(tree)
        set(value) {
            synchronized(tree) {
                if (tree.isOpen()) {
                    throw WinterException(
                        "Cannot set injection adapter because application graph is already open"
                    )
                }
                field = value
            }
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
     * Create and return dependency graph for [instance].
     *
     * @param instance The instance for which a graph should be created.
     * @param block An optional builder block to pass to the component createGraph method.
     * @return The newly created graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun createGraph(instance: Any, block: ComponentBuilderBlock? = null): Graph =
        injectionAdapter.createGraph(instance, block)

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
    fun getGraph(instance: Any): Graph = injectionAdapter.getGraph(instance)

    /**
     * Dispose the dependency graph of the given [instance].
     *
     * @param instance The instance to dispose the graph for.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    fun disposeGraph(instance: Any) {
        injectionAdapter.disposeGraph(instance)
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

    /**
     * Adapter interface to provide application specific graph creation and retrieval strategy.
     */
    interface InjectionAdapter {

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

}
