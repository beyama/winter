package io.jentz.winter.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import io.jentz.winter.*

/**
 * Simple extensible injection adapter that an application component with an "activity" named
 * subcomponent.
 *
 * The adapters [createGraph] method registers the application instance on the application
 * dependency graph and the activity instance on the activity dependency graph.
 *
 * The [createGraph] and [disposeGraph] methods support instances of [Application] and [Activity].
 * The retrieval method [getGraph] supports instances of [Application], [Activity], [View],
 * [DependencyGraphContextWrapper] and [ContextWrapper].
 *
 */
open class SimpleAndroidInjectionAdapter(
    protected val app: WinterApplication
) : WinterInjection.Adapter {

    override fun createGraph(instance: Any, block: ComponentBuilderBlock?): Graph {
        return when (instance) {
            is Application -> app.open {
                constant(app)
                constant(instance)
                constant<Context>(instance)
                block?.invoke(this)
            }
            is Activity -> app.open("activity", identifier = instance) {
                constant(instance)
                constant<Context>(instance)
                block?.invoke(this)
            }
            else -> throw WinterException("Can't create dependency graph for instance <$instance>.")
        }
    }

    override fun getGraph(instance: Any): Graph {
        return when (instance) {
            is Application -> app.get()
            is Activity -> app.get(instance)
            is View -> getGraph(instance.context)
            is DependencyGraphContextWrapper -> instance.graph
            is ContextWrapper -> getGraph(instance.baseContext)
            else -> app.get()
        }
    }

    override fun disposeGraph(instance: Any) {
        when (instance) {
            is Application -> app.close()
            is Activity -> app.close(instance)
        }
    }

}

/**
 * Register a [SimpleAndroidInjectionAdapter] on this [WinterInjection] instance.
 *
 * @param application The [WinterApplication] instance to be used by the adapter.
 */
fun WinterInjection.useSimpleAndroidAdapter(application: WinterApplication = Winter) {
    adapter = SimpleAndroidInjectionAdapter(application)
}
