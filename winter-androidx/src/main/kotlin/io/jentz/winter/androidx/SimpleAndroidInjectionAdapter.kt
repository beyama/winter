package io.jentz.winter.androidx

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
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
 * * the activities [lifecycle][Lifecycle] if the activity implement [LifecycleOwner]
 * * the activity as [LifecycleOwner] if the activity implements that
 * * the activity as [ViewModelStoreOwner] if the activity implements that
 * * the activities [androidx.lifecycle.ViewModelStore] if the activity implements [ViewModelStoreOwner]
 */
open class SimpleAndroidInjectionAdapter(
    protected val app: WinterApplication
) : WinterApplication.InjectionAdapter {

    override fun get(instance: Any): Graph? = when (instance) {
        is DependencyGraphContextWrapper -> instance.graph
        is Application -> getApplicationGraph(instance)
        is Activity -> getActivityGraph(instance)
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
        return getActivityParentGraph(activity)
            ?.getOrOpenSubgraph(ActivityScope::class, activity) {
                setupActivityGraph(activity, this)
            }
    }

    protected open fun getActivityParentGraph(activity: Activity): Graph? = app.graph

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
            else -> throw WinterException("Unsupported type for auto close `$instance`.")
        }
    }

    protected open fun closeActivityGraph(activity: Activity) {
        getActivityParentGraph(activity)?.closeSubgraph(activity)
    }

    protected open fun setupActivityGraph(activity: Activity, builder: Component.Builder) {
        activity as? LifecycleOwner ?: throw WinterException(
            "Activity `$activity` must implement ${LifecycleOwner::class.java.name}"
        )
        setupAutoClose(activity)
        provideAndroidTypes(activity, builder)
    }

    protected open fun provideAndroidTypes(instance: Any, builder: Component.Builder) {
        with(builder) {
            if (instance is Context) {
                constant(instance)
            }
            if (instance is Activity) {
                constant(instance)
            }
            if (instance is LifecycleOwner) {
                constant(instance)
                constant(instance.lifecycle)
            }
            if (instance is ViewModelStoreOwner) {
                constant(instance)
                constant(instance.viewModelStore)
            }
            if (instance is SavedStateRegistryOwner) {
                constant(instance)
                constant(instance.savedStateRegistry)
            }

            if (instance is OnBackPressedDispatcherOwner) {
                constant(instance)
                constant(instance.onBackPressedDispatcher)
            }

            if (instance is ComponentActivity) {
                constant(instance)
            }
        }
    }

    protected fun setupAutoClose(lifecycleOwner: LifecycleOwner) {
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
