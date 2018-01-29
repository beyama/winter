package io.jentz.winter.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import io.jentz.winter.Graph
import io.jentz.winter.GraphRegistry
import io.jentz.winter.Injector
import io.jentz.winter.WinterException
import io.jentz.winter.android.AndroidInjection.Adapter
import io.jentz.winter.internal.MembersInjector

/**
 * Retrieves application and activity graphs and injects into core Android types.
 *
 * An application specific graph creation and retrieval strategy can be provided by setting a custom [Adapter].
 *
 * Example using the [AndroidInjection.PresentationAdapter]:
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
 *     AndroidInjection.adapter = AndroidInjection.PresentationAdapter()
 *
 *     GraphRegistry.applicationComponent = component {
 *       singleton<GitHubApi> { GitHubApiImpl() }
 *
 *         // A presentation subcomponent that survives configuration changes
 *         subcomponent("presentation") {
 *
 *           singleton { RepoListViewModel(instance()) }
 *
 *           // The activity subcomponent that gets recreated with every configuration change
 *           subcomponent("activity") {
 *             singleton { Glide.with(instance<Activity>()) }
 *           }
 *         }
 *       }
 *     }
 *
 *     GraphRegistry.open { constant<Application>(this@MyApplication) }
 *   }
 * }
 *
 * class MyActivity : Activity() {
 *   private val injector = Injector()
 *   private val viewModel: RepoListViewModel by injector.instance()
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *     AndroidInjection.onActivityCreate(this, injector)
 *     super.onCreate(savedInstanceState)
 *   }
 *
 *   override fun onDestroy() {
 *     AndroidInjection.onActivityDestroy(this)
 *     super.onDestroy()
 *   }
 *
 * }
 * ```
 *
 */
object AndroidInjection {

    /**
     * Adapter interface for Android application specific graph creation and retrieval strategy.
     */
    interface Adapter {
        /**
         * Get the application dependency graph.
         */
        fun getApplicationGraph(context: Context): Graph

        /**
         * Get the activity dependency graph for the given [activity].
         */
        fun getActivityGraph(activity: Activity): Graph

        /**
         * Create and return the activity dependency grapgh for the given [activity].
         */
        fun createActivityGraph(activity: Activity): Graph

        /**
         * Dispose the dependency graph of the given [activity].
         */
        fun disposeActivityGraph(activity: Activity)
    }

    /**
     * Adapter that operates on the [GraphRegistry] and requires a root component with an "activity" named
     * subcomponent. The adapter adds the activity instance to the activity dependency graph.
     */
    class SimpleAdapter : Adapter {
        override fun getApplicationGraph(context: Context): Graph = GraphRegistry.get()

        override fun getActivityGraph(activity: Activity): Graph = GraphRegistry.get(activity)

        override fun createActivityGraph(activity: Activity): Graph =
                GraphRegistry.open("activity", identifier = activity) { constant(activity) }

        override fun disposeActivityGraph(activity: Activity) {
            GraphRegistry.close(activity)
        }
    }

    /**
     * Adapter that operates on the [GraphRegistry] and requires a root component with a "presentation" named
     * subcomponent that has a "activity" named subcomponent. The adapter adds the activity instance to the activity
     * dependency graph.
     *
     * The activity graph gets disposed on [AndroidInjection.onActivityDestroy] but the presentation dependency graph
     * gets only disposed when [Activity.isFinishing] returns true so the presentation dependency graph survives
     * configuration changes.
     */
    class PresentationAdapter : Adapter {
        override fun getApplicationGraph(context: Context): Graph = GraphRegistry.get()

        override fun getActivityGraph(activity: Activity): Graph = GraphRegistry.get(activity.javaClass, activity)

        override fun createActivityGraph(activity: Activity): Graph {
            val presentationScope = activity.javaClass
            if (!GraphRegistry.has(presentationScope)) {
                GraphRegistry.open("presentation", identifier = presentationScope)
            }
            return GraphRegistry.open(presentationScope, "activity", identifier = activity) {
                constant(activity)
            }
        }

        override fun disposeActivityGraph(activity: Activity) {
            val presentationScope = activity.javaClass
            if (GraphRegistry.has(presentationScope, activity)) {
                GraphRegistry.close(presentationScope, activity)

                if (activity.isFinishing) {
                    GraphRegistry.close(presentationScope)
                }
            }
        }
    }

    /**
     * Set the application specific [adapter][Adapter].
     * The default adapter is the [SimpleAdapter].
     */
    var adapter: Adapter = SimpleAdapter()

    /**
     * Create and return the activity dependency graph.
     * This is usually called during [Activity.onCreate].
     */
    @JvmStatic
    fun onActivityCreate(activity: Activity): Graph = adapter.createActivityGraph(activity)

    /**
     * Create and return the activity dependency graph and pass the graph to the given [injector].
     * This is usually called during [Activity.onCreate].
     */
    @JvmStatic
    fun onActivityCreate(activity: Activity, injector: Injector): Graph =
            onActivityCreate(activity).also { injector.inject(it) }

    /**
     * Dispose the activity graph.
     * This is usually called during [Activity.onDestroy].
     */
    @JvmStatic
    fun onActivityDestroy(activity: Activity) {
        adapter.disposeActivityGraph(activity)
    }

    /**
     * Get the application graph from [context].
     */
    @JvmStatic
    fun getApplicationGraph(context: Context) = adapter.getApplicationGraph(context)

    /**
     * Get activity graph from [context].
     */
    @JvmStatic
    fun getActivityGraph(context: Context): Graph = adapter.getActivityGraph(getActivity(context))

    /**
     * Get activity graph from [view].
     */
    @JvmStatic
    fun getActivityGraph(view: View): Graph = getActivityGraph(view.context)

    /**
     * Gets activity graph from [view] and calls [Injector.inject] with it and returns the [view].
     */
    @JvmStatic
    fun <T : View> inject(view: T, injector: Injector): T {
        inject(view.context, injector)
        return view
    }

    /**
     * Gets activity graph from [context] and calls [Injector.inject] with it and returns the [context].
     */
    @JvmStatic
    fun <T : Context> inject(context: T, injector: Injector): T {
        injector.inject(getActivityGraph(context))
        return context
    }

    /**
     * Injects into [context] from activity graph and returns context.
     * This uses [MembersInjector] and is useful in conjunction with Winters JSR330 annotation processor.
     */
    @JvmStatic
    @JvmOverloads
    fun <T : Context> inject(context: T, injectSuperClasses: Boolean = false): T {
        return getActivityGraph(context).inject(context, injectSuperClasses)
    }

    /**
     * Injects into [view] from activity graph and returns view.
     * This uses [MembersInjector] and is useful in conjunction with Winters JSR330 annotation processor.
     */
    @JvmStatic
    @JvmOverloads
    fun <T : View> inject(view: T, injectSuperClasses: Boolean = false): T {
        return getActivityGraph(view).inject(view, injectSuperClasses)
    }

    private fun getActivity(context: Context): Activity = when (context) {
        is Activity -> context
        is ContextWrapper -> getActivity(context.baseContext)
        else -> throw WinterException("The given context is not an activity context.")
    }

}