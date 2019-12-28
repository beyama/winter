package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.Tree
import io.jentz.winter.WinterApplication

/**
 * Simple extensible injection adapter that an application component with an "activity" named
 * subcomponent.
 *
 * The adapters [open] method registers the application instance on the application
 * dependency graph and the activity instance on the activity dependency graph.
 *
 * The [open] and [close] methods support instances of [Application] and [Activity].
 * The retrieval method [get] supports instances of [Application], [Activity], [View],
 * [DependencyGraphContextWrapper] and [ContextWrapper].
 *
 */
open class SimpleAndroidInjectionInjectionAdapter(
    protected val tree: Tree
) : WinterApplication.InjectionAdapter {

    override fun open(instance: Any, block: ComponentBuilderBlock?): Graph? {
        return when (instance) {
            is Application -> tree.open {
                constant(instance)
                constant<Context>(instance)
                block?.invoke(this)
            }
            is Activity -> tree.open("activity", identifier = instance) {
                constant(instance)
                constant<Context>(instance)
                block?.invoke(this)
            }
            else -> null
        }
    }

    override fun get(instance: Any): Graph? {
        return when (instance) {
            is Application -> tree.getOrNull()
            is Activity -> tree.getOrNull(instance)
            is View -> get(instance.context)
            is DependencyGraphContextWrapper -> instance.graph
            is ContextWrapper -> get(instance.baseContext)
            else -> null
        }
    }

    override fun close(instance: Any) {
        when (instance) {
            is Application -> tree.close()
            is Activity -> tree.close(instance)
        }
    }

}

/**
 * Register a [SimpleAndroidInjectionInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useSimpleAndroidAdapter() {
    injectionAdapter = SimpleAndroidInjectionInjectionAdapter(tree)
}
