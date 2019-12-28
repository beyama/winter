package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.Tree
import io.jentz.winter.WinterApplication

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
 * The [open] and [close] methods support instances of [Application] and [Activity].
 * The retrieval method [get] supports instances of [Application], [Activity], [View],
 * [DependencyGraphContextWrapper] and [ContextWrapper].
 *
 * The optional builder block for [open] with an Activity is applied to the "activity"
 * component createGraph *NOT* the "presentation" component createGraph.
 */
open class AndroidPresentationScopeInjectionAdapter(
    protected val tree: Tree
) : WinterApplication.InjectionAdapter {

    override fun open(instance: Any, block: ComponentBuilderBlock?): Graph? {
        return when (instance) {
            is Application -> tree.open {
                constant(instance)
                constant<Context>(instance)
                block?.invoke(this)
            }
            is Activity -> {
                val presentationIdentifier = presentationIdentifier(instance)
                tree.getOrOpen("presentation", identifier = presentationIdentifier)
                tree.open(presentationIdentifier, "activity", identifier = instance) {
                    constant(instance)
                    constant<Context>(instance)
                    block?.invoke(this)
                }
            }
            else -> null
        }
    }

    override fun get(instance: Any): Graph? {
        return when (instance) {
            is DependencyGraphContextWrapper -> instance.graph
            is Application -> tree.getOrNull()
            is Activity -> tree.getOrNull(presentationIdentifier(instance), instance)
            is View -> get(instance.context)
            is ContextWrapper -> get(instance.baseContext)
            else -> null
        }
    }

    override fun close(instance: Any) {
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
 * Register an [AndroidPresentationScopeInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useAndroidPresentationScopeAdapter() {
    injectionAdapter = AndroidPresentationScopeInjectionAdapter(tree)
}
