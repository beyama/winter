package io.jentz.winter.android

import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import io.jentz.winter.Graph

/**
 * A [ContextWrapper] that holds a reference to a [Graph] and creates a clone of [LayoutInflater]
 * that is bound to this.
 *
 * This is useful if you need to provide a specific dependency graph to a view hierarchy other than
 * your Activity graph.
 */
class DependencyGraphContextWrapper(base: Context, val graph: Graph) : ContextWrapper(base) {

    companion object {
        /**
         * Use with [getSystemService] to retrieve the [Graph] instance.
         */
        const val WINTER_GRAPH = "winter_graph"
    }

    private val layoutInflater by lazy {
        LayoutInflater.from(baseContext).cloneInContext(this)
    }

    override fun getSystemService(name: String?): Any? = when (name) {
        Context.LAYOUT_INFLATER_SERVICE -> layoutInflater
        WINTER_GRAPH -> graph
        else -> super.getSystemService(name)
    }

}
