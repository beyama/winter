package io.jentz.winter.androidx.viewmodel.savedstate

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import io.jentz.winter.GFactory
import io.jentz.winter.Graph
import java.util.*

@PublishedApi
internal class SavedStateHandleHolder {
    var handles = LinkedList<SavedStateHandle>()
}

@PublishedApi
internal class WinterSavedStateViewModelFactory(
    private val graph: Graph,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle?,
    private val factory: GFactory<ViewModel>
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        val holder: SavedStateHandleHolder = graph.instance()
        holder.handles.push(handle)
        return try {
            factory.invoke(graph) as T
        } finally {
            holder.handles.pop()
        }
    }

}

