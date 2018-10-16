package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import io.jentz.winter.Graph
import io.jentz.winter.GraphRegistry
import io.jentz.winter.Injection
import io.jentz.winter.WinterException

/**
 * Simple extensible injection adapter that operates on the [GraphRegistry] and requires a root
 * component with an "activity" named subcomponent.
 *
 * The adapters [createGraph] method registers the application instance on the application
 * dependency graph and the activity instance on the activity dependency graph.
 *
 * The [createGraph] and [disposeGraph] methods support instances of [Application] and [Activity].
 * The retrieval method [getGraph] supports instances of [Application], [Activity], [View],
 * [DependencyGraphContextWrapper] and [ContextWrapper].
 *
 */
open class SimpleAndroidInjectionAdapter : Injection.Adapter {

    override fun createGraph(instance: Any): Graph {
        return when (instance) {
            is Application -> GraphRegistry.open {
                constant(instance)
                constant<Context>(instance)
            }
            is Activity -> GraphRegistry.open("activity", identifier = instance) {
                constant(instance)
            }
            else -> throw WinterException("Can't create dependency graph for instance <$instance>")
        }
    }

    override fun getGraph(instance: Any): Graph {
        return when (instance) {
            is Application -> GraphRegistry.get()
            is Activity -> GraphRegistry.get(instance)
            is View -> getGraph(instance.context)
            is DependencyGraphContextWrapper -> instance.graph
            is ContextWrapper -> getGraph(instance.baseContext)
            else -> GraphRegistry.get()
        }
    }

    override fun disposeGraph(instance: Any) {
        when (instance) {
            is Application -> GraphRegistry.close()
            is Activity -> GraphRegistry.close(instance)
        }
    }

}
