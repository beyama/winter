package io.jentz.winter.androidx.viewmodel.savedstate

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import io.jentz.winter.Component
import io.jentz.winter.GFactory
import io.jentz.winter.typeKey

/**
 * Register a [ViewModel] factory on this component that requires a
 * [androidx.lifecycle.SavedStateHandle].
 *
 * Be careful, view models outlive activities and therefore shouldn't depend on anything that has
 * the same lifetime as an activity.
 * Since this needs a [androidx.savedstate.SavedStateRegistryOwner] to create the [ViewModel]
 * instance you have to declare those types of view models on your activity scope. Be extra careful
 * to only use dependencies that are part of you application or presentation scope.
 */
inline fun <reified R : ViewModel> Component.Builder.savedStateViewModel(
    override: Boolean = false,
    defaultArgs: Bundle? = null,
    noinline factory: GFactory<R>
) {

    if (!containsKey(typeKey<SavedStateHandleHolder>())) {

        singleton { SavedStateHandleHolder() }

        prototype { instance<SavedStateHandleHolder>().handles.last }

    }

    prototype(override = override) {
        ViewModelProvider(
            instance<ViewModelStore>(),
            WinterSavedStateViewModelFactory(this, instance(), defaultArgs, factory)
        ).get(R::class.java)
    }

}
