package io.jentz.winter.androidx

import android.app.Activity
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.androidx.inject.ActivityScope
import io.jentz.winter.androidx.inject.PresentationScope

/**
 * Extended version of [SimpleAndroidInjectionAdapter] that retains a [PresentationScope] subgraph
 * during Activity re-creation (configuration changes).
 *
 * It expects an application component like:
 *
 * ```
 * Winter.component {
 *   // this sub-graph outlives configuration changes and is only disposed when Activity
 *   // isFinishing == true
 *   subcomponent(PresentationScope::class) {
 *     // this is recreated every time the Activity is recreated
 *     subcomponent(ActivityScope::class) {
 *     }
 *   }
 * }
 * Winter.useAndroidPresentationScopeAdapter()
 * ```
 */
open class AndroidPresentationScopeInjectionAdapter(
    winterApplication: WinterApplication
) : SimpleAndroidInjectionAdapter(winterApplication) {

    override fun getActivityGraph(activity: Activity): Graph? {
        val presentationGraph = getPresentationGraph(activity)

        presentationGraph.getSubgraphOrNull(activity)?.let { return it }

        return presentationGraph.openSubgraph(ActivityScope::class, activity) {
            setupActivityGraph(activity, this)
        }
    }

    override fun closeActivityGraph(activity: Activity) {
        if (activity.isFinishing) {
            getPresentationGraph(activity).close()
        } else {
            getPresentationGraph(activity).closeSubgraph(activity)
        }
    }

    private fun getPresentationGraph(activity: Activity): Graph = app.graph
        .getOrOpenSubgraph(PresentationScope::class, presentationIdentifier(activity))

    private fun presentationIdentifier(activity: Activity) = activity.javaClass

}

/**
 * Register an [AndroidPresentationScopeInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useAndroidPresentationScopeAdapter() {
    injectionAdapter = AndroidPresentationScopeInjectionAdapter(this)
}
