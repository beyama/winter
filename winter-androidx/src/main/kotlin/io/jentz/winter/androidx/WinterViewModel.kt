package io.jentz.winter.androidx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import io.jentz.winter.Graph

/**
 * A [ViewModel] that can hold a [Graph] instance and closes it [onCleared].
 */
class WinterViewModel : ViewModel() {
    companion object {
        inline fun getOrPutGraph(
            owner: ViewModelStoreOwner,
            factory: () -> Graph
        ): Graph {
            val model = ViewModelProvider(owner)[WinterViewModel::class.java]
            model.graph?.let { return it }
            return factory().also { model.graph = it }
        }
    }

    var graph: Graph? = null

    override fun onCleared() {
        graph?.close()
        graph = null
    }
}
