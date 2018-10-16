package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import io.jentz.winter.Graph
import io.jentz.winter.GraphRegistry
import io.jentz.winter.Injection
import io.jentz.winter.WinterException

/**
 * Android injection adapter that is backed by [GraphRegistry] and retains a `presentation` sub
 * graph during Activity re-creation (configuration changes).
 *
 * It expects an application component like:
 *
 * ```
 * GraphRegistry = component {
 *   // this sub-graph outlives configuration changes and is only disposed when Activity
 *   // isFinishing == true
 *   subcomponent("presentation") {
 *     // this is recreated every time the Activity is recreated
 *     subcomponent("activity") {
 *     }
 *   }
 * }
 * ```
 *
 * The adapter registers the [Application] as [Context] and [Application] on the application graph
 * and the [Activity] as [Context] and [Activity] on the "activity" graph.
 *
 * The [createGraph] and [disposeGraph] methods support instances of [Application] and [Activity].
 * The retrieval method [getGraph] supports instances of [Application], [Activity], [View],
 * [DependencyGraphContextWrapper] and [ContextWrapper].
 *
 * [getGraph] called with an unknown type will return the application graph.
 *
 */
open class AndroidPresentationScopeAdapter : Injection.Adapter {

    override fun createGraph(instance: Any): Graph {
        return when (instance) {
            is Application -> GraphRegistry.open {
                constant(instance)
                constant<Context>(instance)
            }
            is Activity -> {
                val presentationIdentifier = presentationIdentifier(instance)
                if (!GraphRegistry.has(presentationIdentifier)) {
                    GraphRegistry.open("presentation", identifier = presentationIdentifier)
                }
                GraphRegistry.open(presentationIdentifier, "activity", identifier = instance) {
                    constant(instance)
                    constant<Context>(instance)
                }
            }
            else -> throw WinterException("Can't create dependency graph for instance <$instance>")
        }
    }

    override fun getGraph(instance: Any): Graph {
        return when (instance) {
            is DependencyGraphContextWrapper -> instance.graph
            is Application -> GraphRegistry.get()
            is Activity -> GraphRegistry.get(presentationIdentifier(instance), instance)
            is View -> getGraph(instance.context)
            is ContextWrapper -> getGraph(instance.baseContext)
            else -> GraphRegistry.get()
        }
    }

    override fun disposeGraph(instance: Any) {
        when (instance) {
            is Application -> GraphRegistry.close()
            is Activity -> {
                if (instance.isFinishing) {
                    GraphRegistry.close(presentationIdentifier(instance))
                } else {
                    GraphRegistry.close(presentationIdentifier(instance), instance)
                }
            }
        }
    }

    private fun presentationIdentifier(activity: Activity) = activity.javaClass.name

}
