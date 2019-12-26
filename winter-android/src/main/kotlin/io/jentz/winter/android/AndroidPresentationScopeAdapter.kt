package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import io.jentz.winter.*

/**
 * Android injection adapter that retains a `presentation` sub graph during Activity
 * re-creation (configuration changes).
 *
 * It expects an application component like:
 *
 * ```
 * Winter.component {
 *   // this sub-graph outlives configuration changes and is only disposed when Activity
 *   // isFinishing == true
 *   subcomponent("presentation") {
 *     // this is recreated every time the Activity is recreated
 *     subcomponent("activity") {
 *     }
 *   }
 * }
 * Injection.useAndroidPresentationScopeAdapter()
 * ```
 *
 * The adapter registers the [Application] as [Context] and [Application] on the application graph
 * and the [Activity] as [Context] and [Activity] on the "activity" graph.
 *
 * The [createGraph] and [disposeGraph] methods support instances of [Application] and [Activity].
 * The retrieval method [getGraph] supports instances of [Application], [Activity], [View],
 * [DependencyGraphContextWrapper] and [ContextWrapper].
 *
 * The optional builder block for [createGraph] with an Activity is applied to the "activity"
 * component createGraph *NOT* the "presentation" component createGraph.
 *
 * [getGraph] called with an unknown type will return the application graph.
 *
 */
open class AndroidPresentationScopeAdapter(
    protected val tree: Tree
) : WinterInjection.Adapter {

    override fun createGraph(instance: Any, block: ComponentBuilderBlock?): Graph {
        return when (instance) {
            is Application -> tree.open {
                constant(instance)
                constant<Context>(instance)
                block?.invoke(this)
            }
            is Activity -> {
                val presentationIdentifier = presentationIdentifier(instance)
                if (!tree.has(presentationIdentifier)) {
                    tree.open("presentation", identifier = presentationIdentifier)
                }
                tree.open(presentationIdentifier, "activity", identifier = instance) {
                    constant(instance)
                    constant<Context>(instance)
                    block?.invoke(this)
                }
            }
            else -> throw WinterException("Can't create dependency graph for instance <$instance>.")
        }
    }

    override fun getGraph(instance: Any): Graph {
        return when (instance) {
            is DependencyGraphContextWrapper -> instance.graph
            is Application -> tree.get()
            is Activity -> tree.get(presentationIdentifier(instance), instance)
            is View -> getGraph(instance.context)
            is ContextWrapper -> getGraph(instance.baseContext)
            else -> tree.get()
        }
    }

    override fun disposeGraph(instance: Any) {
        when (instance) {
            is Application -> tree.close()
            is Activity -> {
                if (instance.isFinishing) {
                    tree.close(presentationIdentifier(instance))
                } else {
                    tree.close(presentationIdentifier(instance), instance)
                }
            }
        }
    }

    private fun presentationIdentifier(activity: Activity) = activity.javaClass

}

/**
 * Register an [AndroidPresentationScopeAdapter] on this [WinterInjection] instance.
 *
 * @param application The [WinterApplication] instance to be used by the adapter.
 */
fun WinterInjection.useAndroidPresentationScopeAdapter(application: WinterApplication = Winter) {
    adapter = AndroidPresentationScopeAdapter(application.tree)
}
