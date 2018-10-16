package io.jentz.winter.android

import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import io.jentz.winter.Graph

/**
 * A [ContextWrapper] that holds a reference to a [Graph].
 */
class DependencyGraphContextWrapper(base: Context, val graph: Graph) : ContextWrapper(base) {

    private val layoutInflater by lazy {
        LayoutInflater.from(baseContext).cloneInContext(this)
    }

    override fun getSystemService(name: String?): Any =
        if (name == Context.LAYOUT_INFLATER_SERVICE) {
            layoutInflater
        } else {
            super.getSystemService(name)
        }

}
