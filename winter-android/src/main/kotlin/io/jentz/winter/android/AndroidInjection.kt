package io.jentz.winter.android

import android.content.Context
import io.jentz.winter.Graph
import io.jentz.winter.Injection
import io.jentz.winter.Injector
import io.jentz.winter.MembersInjector

/**
 * Retrieves application and activity graphs and injects into core Android types.
 *
 * An application specific graph creation and retrieval strategy can be provided by setting a custom
 * [Adapter].
 *
 * Example using the default [SimpleAndroidInjectionAdapter]:
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
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
 *     AndroidInjection.createGraph(this)
 *   }
 * }
 *
 * class MyActivity : Activity() {
 *   private val injector = Injector()
 *   private val viewModel: RepoListViewModel by injector.instance()
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     AndroidInjection.createGraphAndInject(this, injector)
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     AndroidInjection.disposeGraph(this)
 *     super.onDestroy()
 *   }
 *
 * }
 * ```
 *
 * To register a custom graph creation and retrieval strategy a custom [AndroidInjection.Adapter]
 * can be registered by
 * setting the [AndroidInjection.adapter] property.
 *
 * ```
 * AndroidInjection.adapter = MyCustomAdapter()
 * ```
 */
@Deprecated(
    message = "Use io.jentz.winter.Injection instead",
    replaceWith = ReplaceWith("Injection", "io.jentz.winter.Injection")
)
object AndroidInjection {

    /**
     * Adapter interface for Android application specific graph creation and retrieval strategy.
     */
    @Deprecated("Use io.jentz.winter.Injection.Adapter")
    interface Adapter : Injection.Adapter

    /**
     * Set the application specific [adapter][Adapter].
     * The default adapter is the [SimpleAndroidInjectionAdapter].
     */
    var adapter: Injection.Adapter = SimpleAndroidInjectionAdapter()

    /**
     * Create and return dependency graph for [instance].
     *
     * @param instance The instance for which a graph should be created.
     * @return The newly created graph.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    @JvmStatic
    fun createGraph(instance: Any): Graph = adapter.createGraph(instance, null)

    /**
     * Create and return dependency graph for [instance] and also pass the graph to the given
     * [injector].
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
     * Create and return dependency graph for [instance] and also inject member.
     *
     * This is useful in conjunction with JSR330 `Inject` annotations.
     *
     * @param instance The instance to create a graph for and to inject into.
     * @param injectSuperClasses If true this will look for members injectors for super classes
     *                           too.
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
     * Get application dependency graph.
     *
     * Alias for `AndroidInjection.getGraph(context.applicationContext)`.
     *
     * @param context The context to get the application graph from.
     * @return The application dependency graph.
     * @throws [io.jentz.winter.WinterException] if application dependency graph doesn't exist.
     */
    @JvmStatic
    fun getApplicationGraph(context: Context): Graph = getGraph(context.applicationContext)

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
     * This uses [MembersInjector] and is useful in conjunction with Winters JSR330 annotation
     * processor.
     *
     * @param instance The instance to retrieve the dependency graph for and inject dependencies
     *                 into.
     * @param injectSuperClasses If true this will look for members injectors for super classes too.
     * @throws [io.jentz.winter.WinterException] if given [instance] type is not supported.
     */
    @JvmStatic
    @JvmOverloads
    fun <T : Any> inject(instance: T, injectSuperClasses: Boolean = false) {
        getGraph(instance).inject(instance, injectSuperClasses)
    }

}
