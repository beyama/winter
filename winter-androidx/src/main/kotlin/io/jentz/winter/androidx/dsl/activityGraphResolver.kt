package io.jentz.winter.androidx.dsl

import androidx.activity.ComponentActivity
import io.jentz.winter.Component
import io.jentz.winter.androidx.ActivityScope
import io.jentz.winter.androidx.ViewModelScope
import io.jentz.winter.androidx.WinterViewModel
import io.jentz.winter.androidx.closeWithLifecycle
import io.jentz.winter.delegate.graphResolver

inline fun <reified A: ComponentActivity> Component.Builder.activityGraphResolver(
    noinline viewModelScope: (Component.Builder.(A) -> Unit)? = null,
    noinline activityScope: (Component.Builder.(A) -> Unit)? = null
) {
    graphResolver<A> { graph, activity ->
        val viewModelGraph = WinterViewModel.getOrPutGraph(activity) {
            graph.createSubgraph(ViewModelScope) {
                viewModelScope?.let { it(activity) }
            }
        }
        viewModelGraph
            .createSubgraph(ActivityScope) {
                activity(activity)
                activityScope?.let { it(activity) }
            }
            .closeWithLifecycle(activity.lifecycle)
    }
}