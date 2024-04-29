package io.jentz.winter.androidx.dsl

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistryOwner
import io.jentz.winter.Component
import io.jentz.winter.typeKey

inline fun <reified A: Activity> Component.Builder.activity(activity: A) {
    constant<Context>(activity)
    constant<Activity>(activity)

    if (activity is LifecycleOwner) {
        constant(activity.lifecycle)
        constant(activity.lifecycleScope)
    }

    if (activity is ViewModelStoreOwner) {
        constant<ViewModelStoreOwner>(activity)
        constant(activity.viewModelStore)
    }

    if (activity is SavedStateRegistryOwner) {
        constant<SavedStateRegistryOwner>(activity)
        constant(activity.savedStateRegistry)
    }

    if (activity is OnBackPressedDispatcherOwner) {
        constant<OnBackPressedDispatcherOwner>(activity)
        constant(activity.onBackPressedDispatcher)
    }

    if (activity is ComponentActivity && !containsKey(typeKey<ComponentActivity>())) {
        constant<ComponentActivity>(activity)
    }

    if (!containsKey(typeKey<A>())) {
        constant<A>(activity)
    }
}