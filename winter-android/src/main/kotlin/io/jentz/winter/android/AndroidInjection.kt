package io.jentz.winter.android

import android.content.Context
import io.jentz.winter.Graph
import io.jentz.winter.Injector
import io.jentz.winter.android.AndroidInjection.Adapter
import io.jentz.winter.internal.MembersInjector

/**
 * Retrieves application and activity graphs and injects into core Android types.
 *
 * An application specific graph creation and retrieval strategy can be provided by setting a custom [Adapter].
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
 *     AndroidInjection.createGraph(this, injector)
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
 * To register a custom graph creation and retrieval strategy a custom [AndroidInjection.Adapter] can be registered by
 * setting the [AndroidInjection.adapter] property.
 *
 * ```
 * AndroidInjection.adapter = MyCustomAdapter()
 * ```
 */
object AndroidInjection {

    /**
     * Adapter interface for Android application specific graph creation and retrieval strategy.
     */
    interface Adapter {

        /**
         * Get dependency graph for [instance].
         */
        fun getGraph(instance: Any): Graph

        /**
         * Create dependency graph for [instance].
         *
         * The adapter implementation is responsible for storing the created graph.
         */
        fun createGraph(instance: Any): Graph

        /**
         * Dispose the dependency graph of the given [instance].
         */
        fun disposeGraph(instance: Any)
    }

    /**
     * Set the application specific [adapter][Adapter].
     * The default adapter is the [SimpleAndroidInjectionAdapter].
     */
    var adapter: Adapter = SimpleAndroidInjectionAdapter()

    /**
     * Get application dependency graph.
     *
     * Sugar for `AndroidInjection.getGraph(context.applicationContext)`.
     */
    @JvmStatic
    fun getApplicationGraph(context: Context): Graph = getGraph(context.applicationContext)

    /**
     * Create and return dependency graph for [instance].
     */
    @JvmStatic
    fun createGraph(instance: Any): Graph = adapter.createGraph(instance)

    /**
     * Create and return dependency graph for [instance] and also pass the graph to the given [injector].
     */
    @JvmStatic
    fun createGraph(instance: Any, injector: Injector): Graph = createGraph(instance).also(injector::inject)

    /**
     * Get dependency graph for [instance].
     */
    @JvmStatic
    fun getGraph(instance: Any): Graph = adapter.getGraph(instance)

    /**
     * Dispose the dependency graph of the given [instance].
     */
    @JvmStatic
    fun disposeGraph(instance: Any) {
        adapter.disposeGraph(instance)
    }

    /**
     * Get dependency graph for given [instance] and inject dependencies into injector.
     */
    @JvmStatic
    fun inject(instance: Any, injector: Injector) {
        injector.inject(getGraph(instance))
    }

    /**
     * Inject into [instance] by using the dependency graph of the [instance].
     * This uses [MembersInjector] and is useful in conjunction with Winters JSR330 annotation processor.
     */
    @JvmStatic
    @JvmOverloads
    fun <T : Any> inject(instance: T, injectSuperClasses: Boolean = false) {
        getGraph(instance).inject(instance, injectSuperClasses)
    }

}