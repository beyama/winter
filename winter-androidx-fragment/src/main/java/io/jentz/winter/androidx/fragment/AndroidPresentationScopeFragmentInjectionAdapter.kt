package io.jentz.winter.androidx.fragment

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.WinterException
import io.jentz.winter.androidx.inject.PresentationScope
import io.jentz.winter.androidx.viewmodel.GraphViewModel

/**
 * Extended version of [SimpleAndroidFragmentInjectionAdapter] that retains a [PresentationScope]
 * subgraph during Activity re-creation (configuration changes).
 *
 * In addition to the types provided by the base adapter this one provides:
 * * the activity as [FragmentActivity] if the activity is an instance of [FragmentActivity]
 * * the activities [androidx.fragment.app.FragmentManager] if the activity is an instance of
 *   [FragmentActivity]
 * * a [WinterFragmentFactory] if [enableWinterFragmentFactory] is true and activity is an
 *   instance of [FragmentActivity]
 */
open class AndroidPresentationScopeFragmentInjectionAdapter(
    app: WinterApplication,
    enableWinterFragmentFactory: Boolean = false
) : SimpleAndroidFragmentInjectionAdapter(app, enableWinterFragmentFactory) {

    override fun getActivityParentGraph(activity: Activity): Graph {
        activity as? ViewModelStoreOwner ?: throw WinterException(
            "Activity `${activity.javaClass.name}` must implement ViewModelStoreOwner"
        )
        val model = ViewModelProvider(activity).get(GraphViewModel::class.java)

        model.graph?.let { return it }

        return app.graph.getOrOpenSubgraph(PresentationScope::class, model) {
            constant(activity.viewModelStore)
        }.also {
            model.graph = it
        }
    }

}

/**
 * Register an [AndroidPresentationScopeFragmentInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useAndroidPresentationScopeFragmentAdapter(
    enableWinterFragmentFactory: Boolean = false
) {
    injectionAdapter = AndroidPresentationScopeFragmentInjectionAdapter(
        this, enableWinterFragmentFactory
    )
}
