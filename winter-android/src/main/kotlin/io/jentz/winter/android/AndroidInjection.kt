package io.jentz.winter.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import io.jentz.winter.Graph
import io.jentz.winter.Injector
import io.jentz.winter.WinterException

object AndroidInjection {

    private var applicationGraphGetter: (Context) -> Graph = { context ->
        (context.applicationContext as ApplicationDependencyGraphHolder).applicationDependencyGraph
    }

    private var activityGraphGetter: (Context) -> Graph = { context -> getActivityDependencyGraph(context) }

    private var activityGraphFactory: (Activity) -> Graph = { activity ->
        applicationGraphGetter(activity).initSubcomponent("activity") { constant(activity) }
    }

    fun configure(applicationGraphGetter: (Context) -> Graph = this.applicationGraphGetter,
                  activityGraphGetter: (Context) -> Graph = this.activityGraphGetter,
                  activityGraphFactory: (Activity) -> Graph = this.activityGraphFactory) {
        this.applicationGraphGetter = applicationGraphGetter
        this.activityGraphGetter = activityGraphGetter
        this.activityGraphFactory = activityGraphFactory
    }

    @JvmStatic
    fun getApplicationGraph(context: Context) = applicationGraphGetter(context)

    @JvmStatic
    fun createActivityGraph(activity: Activity): Graph = activityGraphFactory(activity)

    @JvmStatic
    fun getActivityGraph(context: Context): Graph = activityGraphGetter(context)

    @JvmStatic
    fun inject(view: View, injector: Injector) {
        inject(view.context, injector)
    }

    @JvmStatic
    fun inject(context: Context, injector: Injector) {
        injector.inject(activityGraphGetter(context))
    }

    internal fun getActivityDependencyGraph(context: Context): Graph = when (context) {
        is ActivityDependencyGraphHolder -> context.activityDependencyGraph
        is ContextWrapper -> getActivityDependencyGraph(context.baseContext)
        else -> throw WinterException("Activity graph not found.")
    }

}