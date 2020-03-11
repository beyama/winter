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
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.WinterException
import io.jentz.winter.androidx.inject.ActivityScope

/**
 * Extensible injection adapter that operates on an [io.jentz.winter.inject.ApplicationScope]
 * component with an [ActivityScope] subcomponent.
 *
 * The adapters opens the application component for [Application] and the "activity" component
 * for [Activity]. The [Activity] must implement [LifecycleOwner] so that the opened activity graph
 * can be automatically closed.
 *
 * The following is provided via the activity graph:
 * * the activity as [Context]
 * * the activity as [Activity]
 * * the activity [lifecycle][Lifecycle]
 * * the activity as [FragmentActivity] if it is an instance of [FragmentActivity]
 * * the activity [FragmentManager] if it is an instance of [FragmentActivity]
 *   instance of [FragmentActivity]
 */
open class SimpleAndroidInjectionAdapter(
    protected val app: WinterApplication
) : WinterApplication.InjectionAdapter {

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

    protected open fun getApplicationGraph(application: Application): Graph? =
        app.getOrOpenGraph {
            constant(application)
            constant<Context>(application)
        }

    protected open fun getActivityGraph(activity: Activity): Graph? {
        return getActivityParentGraph(activity).getOrOpenSubgraph(ActivityScope::class, activity) {
            setupActivityGraph(activity, this)
        }
    }

    protected open fun getActivityParentGraph(activity: Activity): Graph = app.graph

    protected open fun getFragmentGraph(fragment: Fragment): Graph? =
        get(fragment.requireActivity())

    protected open fun getViewGraph(view: View): Graph? = get(view.context)

    protected open fun getBroadcastReceiverGraph(receiver: BroadcastReceiver): Graph? =
        app.graph

    protected open fun getContentProviderGraph(contentProvider: ContentProvider): Graph? =
        app.graph

    protected open fun getServiceGraph(service: Service): Graph? =
        app.graph

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
        getActivityParentGraph(activity).closeSubgraph(activity)
    }

    protected open fun closeFragmentGraph(fragment: Fragment) {
    }

    protected open fun setupActivityGraph(activity: Activity, builder: Component.Builder) {
        activity as? LifecycleOwner ?: throw WinterException(
            "Activity `$activity` must implement ${LifecycleOwner::class.java.name}"
        )

        setupAutoClose(activity)

        builder.apply {
            constant<Context>(activity)
            constant<Activity>(activity)
            constant(activity.lifecycle)

            if (activity is FragmentActivity) {
                constant(activity)
                constant(activity.supportFragmentManager)
            }
        }
    }

    private fun setupAutoClose(lifecycleOwner: LifecycleOwner) {
        val closeEvent: Lifecycle.Event = when (lifecycleOwner.lifecycle.currentState) {
            Lifecycle.State.INITIALIZED -> Lifecycle.Event.ON_DESTROY
            Lifecycle.State.CREATED -> Lifecycle.Event.ON_STOP
            Lifecycle.State.STARTED -> Lifecycle.Event.ON_PAUSE
            Lifecycle.State.RESUMED, Lifecycle.State.DESTROYED -> {
                throw WinterException("Cannot setup lifecycle auto close after onResume")
            }
        }
        lifecycleOwner.lifecycle.addObserver(object : LifecycleAutoClose(closeEvent) {
            override fun close() {
                close(lifecycleOwner)
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
