package io.jentz.winter.androidx

import androidx.lifecycle.ViewModel
import io.jentz.winter.Graph

/**
 * A ViewModel that can hold a [Graph] instance and closes it [onCleared].
 *
 * This is intended to be used inside an [io.jentz.winter.WinterApplication.InjectionAdapter] to
 * hold graphs that outlive configuration changes.
 */
class WinterViewModel : ViewModel() {

    var graph: Graph? = null

    override fun onCleared() {
        graph?.close()
        graph = null
    }
}
