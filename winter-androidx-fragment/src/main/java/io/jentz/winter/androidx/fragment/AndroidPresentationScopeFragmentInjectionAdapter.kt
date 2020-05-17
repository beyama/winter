package io.jentz.winter.androidx.fragment

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.savedstate.SavedStateRegistryOwner
import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import io.jentz.winter.androidx.AndroidPresentationScopeInjectionAdapter

/**
 * Extended version of [AndroidPresentationScopeInjectionAdapter] that has support for fragments and
 * provides a variety of subtypes and components of the activity graphs activity.
 *
 * In addition to the types provided by the base adapter this one provides:
 * * the activity as [SavedStateRegistryOwner] if the activity implements that
 * * the activities [androidx.savedstate.SavedStateRegistry] if the activity implements
 *   [SavedStateRegistryOwner]
 * * the activity as [OnBackPressedDispatcherOwner] if the activity implements that
 * * the activities [androidx.activity.OnBackPressedDispatcher] if the activity implements
 *   [OnBackPressedDispatcherOwner]
 * * the activity as [ComponentActivity] if the activity is an instance of [ComponentActivity]
 * * the activity as [FragmentActivity] if the activity is an instance of [FragmentActivity]
 * * the activities [androidx.fragment.app.FragmentManager] if the activity is an instance of
 *   [FragmentActivity]
 * * a [WinterFragmentFactory] if [enableWinterFragmentFactory] is true and activity is an
 *   instance of [FragmentActivity]
 */
open class AndroidPresentationScopeFragmentInjectionAdapter(
    app: WinterApplication,
    private val enableWinterFragmentFactory: Boolean = false
) : AndroidPresentationScopeInjectionAdapter(app) {

    override fun get(instance: Any): Graph? {
        if (instance is Fragment) return getFragmentGraph(instance)
        return super.get(instance)
    }

    protected open fun getFragmentGraph(fragment: Fragment): Graph? =
        get(fragment.requireActivity())

    override fun provideAndroidTypes(instance: Any, builder: Component.Builder) {
        super.provideAndroidTypes(instance, builder)
        exportAndroidTypes(instance, enableWinterFragmentFactory, builder)
    }

}

/**
 * Register an [AndroidPresentationScopeFragmentInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useAndroidPresentationScopeFragmentAdapter(
    enableWinterFragmentFactory: Boolean = false
) {
    injectionAdapter = AndroidPresentationScopeFragmentInjectionAdapter(
        this, enableWinterFragmentFactory
    )
}
