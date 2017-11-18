package io.jentz.winter.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import io.jentz.winter.Graph
import io.jentz.winter.Injector
import io.jentz.winter.WinterException
import io.jentz.winter.internal.MembersInjector

/**
 * Retrieves application and activity graphs and injects into core Android types.
 *
 * The default application graph retrieval strategy is to assume that the application context implements
 * [ApplicationDependencyGraphHolder] and to get the graph from there.
 *
 * The default activity graph retrieval strategy is to assume that the activity implements
 * [ActivityDependencyGraphHolder] and to get the graph from there.
 *
 * The default strategy to create a new activity graph is to assume that the activity component is a "activity" named
 * subcomponent of the application component and retrieve the application graph and call [Graph.initSubcomponent]
 * and add the activity instance as a constant of type [Activity] to the resulting activity graph.
 *
 */
object AndroidInjection {

    private var applicationGraphGetter: (Context) -> Graph = { context ->
        (context.applicationContext as ApplicationDependencyGraphHolder).applicationDependencyGraph
    }

    private var activityGraphGetter: (Context) -> Graph = { context -> getActivityDependencyGraph(context) }

    private var activityGraphFactory: (Activity) -> Graph = { activity ->
        applicationGraphGetter(activity).initSubcomponent("activity") { constant(activity) }
    }

    /**
     * Configure this to use different graph retrieval and creation strategies.
     */
    fun configure(applicationGraphGetter: (Context) -> Graph = this.applicationGraphGetter,
                  activityGraphGetter: (Context) -> Graph = this.activityGraphGetter,
                  activityGraphFactory: (Activity) -> Graph = this.activityGraphFactory) {
        this.applicationGraphGetter = applicationGraphGetter
        this.activityGraphGetter = activityGraphGetter
        this.activityGraphFactory = activityGraphFactory
    }

    /**
     * Get the application graph from [context].
     */
    @JvmStatic
    fun getApplicationGraph(context: Context) = applicationGraphGetter(context)

    /**
     * Create a new activity graph for [activity].
     */
    @JvmStatic
    fun createActivityGraph(activity: Activity): Graph = activityGraphFactory(activity)

    /**
     * Get activity graph from [context].
     */
    @JvmStatic
    fun getActivityGraph(context: Context): Graph = activityGraphGetter(context)

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
        injector.inject(activityGraphGetter(context))
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

    internal fun getActivityDependencyGraph(context: Context): Graph = when (context) {
        is ActivityDependencyGraphHolder -> context.activityDependencyGraph
        is ContextWrapper -> getActivityDependencyGraph(context.baseContext)
        else -> throw WinterException("Activity graph not found.")
    }

}