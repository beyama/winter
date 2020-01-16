package io.jentz.winter.androidx

import android.app.Activity
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
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

    override fun closeActivityGraph(activity: Activity) {
        if (activity.isFinishing) {
            getActivityParentGraph(activity).close()
        } else {
            getActivityParentGraph(activity).closeSubgraph(activity)
        }
    }

    override fun getActivityParentGraph(activity: Activity): Graph =
        app.graph.getOrOpenSubgraph(PresentationScope::class, activity.javaClass)

}

/**
 * Register an [AndroidPresentationScopeInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useAndroidPresentationScopeAdapter() {
    injectionAdapter = AndroidPresentationScopeInjectionAdapter(this)
}
