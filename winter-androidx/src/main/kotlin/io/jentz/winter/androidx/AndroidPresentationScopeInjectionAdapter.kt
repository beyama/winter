package io.jentz.winter.androidx

import android.app.Activity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.WinterException
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

    override fun getActivityParentGraph(activity: Activity): Graph {
        activity as? ViewModelStoreOwner ?: throw WinterException(
            "Activity `${activity.javaClass.name}` must implement ViewModelStoreOwner"
        )

        val model = ViewModelProvider(activity).get(WinterViewModel::class.java)

        model.graph?.let { return it }

        return app.graph.createSubgraph(PresentationScope::class).also {
            model.graph = it
        }
    }

}

/**
 * Register an [AndroidPresentationScopeInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useAndroidPresentationScopeAdapter() {
    injectionAdapter = AndroidPresentationScopeInjectionAdapter(this)
}
