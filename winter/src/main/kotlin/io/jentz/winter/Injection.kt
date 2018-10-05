package io.jentz.winter

/**
 * Abstraction to create, get and dispose a dependency graph from a class that can't make use of
 * constructor injection. This takes the burden off of the class to know how exactly a graph
 * or parent graph is stored and how to create and store a new graph.
 *
 * An application specific graph creation and retrieval strategy can be provided by setting a
 * custom [Adapter].
 *
 * ```
 * Injection.adapter = MyCustomAdapter()
 * ```
 *
 * Example using the included SimpleAndroidInjectionAdapter:
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
 *     super.onCreate()
 *
 *     Injection.adapter = SimpleAndroidInjectionAdapter()
 *
 *     GraphRegistry.applicationComponent = component {
 *       singleton<GitHubApi> { GitHubApiImpl() }
 *
 *       singleton { RepoListViewModel(instance()) }
 *
 *       subcomponent("activity") {
 *          singleton { Glide.with(instance<Activity>()) }
 *       }
 *     }
 *
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
object Injection {

    /**
     * Adapter interface to provide application specific graph creation and retrieval strategy.
     */
    interface Adapter {

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
         * @return The newly created graph
         * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
         *
         */
        fun createGraph(instance: Any): Graph

        /**
         * Dispose the dependency graph of the given [instance].
         *
         * @param instance The instance to dispose the graph for.
         * @throws [io.jentz.winter.WinterException] if no graph for this [instance] type exists.
         */
        fun disposeGraph(instance: Any)
    }

    /**
     * Simple adapter for application with only one dependency graph.
     *
     * Register your application component on [GraphRegistry.applicationComponent].
     */
    class ApplicationGraphOnlyAdapter : Adapter {
        override fun getGraph(instance: Any): Graph = GraphRegistry.get()

        override fun createGraph(instance: Any): Graph = GraphRegistry.create()

        override fun disposeGraph(instance: Any) {
            GraphRegistry.close()
        }
    }

    /**
     * Set the application specific [adapter][Adapter].
     *
     * Default adapter is [ApplicationGraphOnlyAdapter].
     */
    var adapter: Adapter = ApplicationGraphOnlyAdapter()

    /**
     * Create and return dependency graph for [instance].
     *
     * @param instance The instance for which a graph should be created.
     * @return The newly created graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    @JvmStatic
    fun createGraph(instance: Any): Graph = adapter.createGraph(instance)

    /**
     * Create and return dependency graph for [instance] and also pass the graph to the given [injector].
     *
     * @param instance The instance for which a graph should be created.
     * @param injector The injector to inject into.
     * @return The created dependency graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    @JvmStatic
    fun createGraphAndInject(instance: Any, injector: Injector): Graph =
            createGraph(instance).also(injector::inject)

    /**
     * Create and return dependency graph for [instance] and also pass the graph to the given [injector].
     *
     * This is useful in conjunction with JSR330 `Inject` annotations.
     *
     * @param instance The instance to create a graph for and to inject into.
     * @param injectSuperClasses  If true this will look for members injectors for super classes too.
     * @return The created dependency graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    @JvmStatic
    @JvmOverloads
    fun <T : Any> createGraphAndInject(instance: T, injectSuperClasses: Boolean = false): Graph =
            createGraph(instance).also { graph -> graph.inject(instance, injectSuperClasses) }

    /**
     * Get dependency graph for [instance].
     *
     * @param instance The instance to retrieve the dependency graph for.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     *
     */
    @JvmStatic
    fun getGraph(instance: Any): Graph = adapter.getGraph(instance)

    /**
     * Dispose the dependency graph of the given [instance].
     *
     * @param instance The instance to dispose the graph for.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    @JvmStatic
    fun disposeGraph(instance: Any) {
        adapter.disposeGraph(instance)
    }

    /**
     * Get dependency graph for given [instance] and inject dependencies into injector.
     *
     * @param instance The instance to retrieve the dependency graph for.
     * @param injector The injector to inject into.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    @JvmStatic
    fun inject(instance: Any, injector: Injector) {
        injector.inject(getGraph(instance))
    }

    /**
     * Inject into [instance] by using the dependency graph of the [instance].
     * This uses [MembersInjector] and is useful in conjunction with Winters JSR330 annotation processor.
     *
     * @param instance The instance to retrieve the dependency graph for and inject dependencies into.
     * @param injectSuperClasses  If true this will look for members injectors for super classes too.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    @JvmStatic
    @JvmOverloads
    fun <T : Any> inject(instance: T, injectSuperClasses: Boolean = false) {
        getGraph(instance).inject(instance, injectSuperClasses)
    }

}