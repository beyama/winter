package io.jentz.winter.androidx.fragment

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.fragment.app.FragmentActivity
import androidx.savedstate.SavedStateRegistryOwner
import io.jentz.winter.Component

internal fun exportAndroidTypes(instance: Any, builder: Component.Builder) {
    with(builder) {
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

        if (instance is FragmentActivity) {
            constant(instance)
            constant(instance.supportFragmentManager)
        }
    }
}
