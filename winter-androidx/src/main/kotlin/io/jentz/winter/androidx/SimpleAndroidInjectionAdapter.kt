package io.jentz.winter.androidx

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.WinterException

/**
 * Extensible injection adapter that operates on an application component with an "activity"
 * named subcomponent.
 *
 * The adapters opens the application component for [Application] and the "activity" component
 * for [Activity]. The [Activity] must implement [LifecycleOwner] so that the opened activity graph
 * can be automatically closed.
 */

open class SimpleAndroidInjectionAdapter(
    protected val winterApplication: WinterApplication
) : WinterApplication.InjectionAdapter {

    protected val tree = winterApplication.tree

    override fun get(instance: Any): Graph? = when (instance) {
        is DependencyGraphContextWrapper -> instance.graph
        is Application -> getApplicationGraph(instance)
        is Activity -> getActivityGraph(instance)
        is Fragment -> getFragmentGraph(instance)
        is View -> getViewGraph(instance)
        is BroadcastReceiver -> getBroadcastReceiverGraph(instance)
        is ContentProvider -> getContentProviderGraph(instance)
        is Service -> getServiceGraph(instance)
        is ContextWrapper -> getContextWrapperGraph(instance)
        else -> null
    }

    protected open fun getApplicationGraph(application: Application): Graph? = tree.getOrOpen {
        constant(application)
        constant<Context>(application)
    }

    protected open fun getActivityGraph(activity: Activity): Graph? {
        tree.getOrNull(activity)?.let { return it }

        setupAutoClose(activity)

        return tree.open("activity", identifier = activity) {
            constant(activity)
            constant<Context>(activity)
        }
    }

    protected open fun getFragmentGraph(fragment: Fragment): Graph? =
        get(fragment.requireActivity())

    protected open fun getViewGraph(view: View): Graph? = get(view.context)

    protected open fun getBroadcastReceiverGraph(receiver: BroadcastReceiver): Graph? =
        tree.get()

    protected open fun getContentProviderGraph(contentProvider: ContentProvider): Graph? =
        tree.get()

    protected open fun getServiceGraph(service: Service): Graph? =
        tree.get()

    protected open fun getContextWrapperGraph(contextWrapper: ContextWrapper): Graph? =
        get(contextWrapper.baseContext)

    protected open fun close(instance: Any) {
        when (instance) {
            is Activity -> closeActivityGraph(instance)
            is Fragment -> closeFragmentGraph(instance)
            else -> throw WinterException("Unsupported type for auto close `$instance`.")
        }
    }

    protected open fun closeActivityGraph(activity: Activity) {
        tree.close(activity)
    }

    protected open fun closeFragmentGraph(fragment: Fragment) {
    }

    protected fun setupAutoClose(instance: Any) {
        val owner = instance as? LifecycleOwner ?: throw WinterException(
            "Instance `$instance` must implement ${LifecycleOwner::class.java.name}"
        )
        val closeEvent: Lifecycle.Event = when (owner.lifecycle.currentState) {
            Lifecycle.State.INITIALIZED -> Lifecycle.Event.ON_DESTROY
            Lifecycle.State.CREATED -> Lifecycle.Event.ON_STOP
            Lifecycle.State.STARTED -> Lifecycle.Event.ON_PAUSE
            Lifecycle.State.RESUMED, Lifecycle.State.DESTROYED -> {
                throw WinterException("Cannot setup lifecycle auto close after onResume")
            }
        }
        owner.lifecycle.addObserver(object : LifecycleAutoClose(closeEvent) {
            override fun close() {
                close(instance)
            }
        })
    }

}

/**
 * Register an [SimpleAndroidInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useSimpleAndroidAdapter() {
    injectionAdapter = SimpleAndroidInjectionAdapter(this)
}
