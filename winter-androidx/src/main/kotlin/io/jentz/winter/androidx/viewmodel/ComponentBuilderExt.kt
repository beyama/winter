package io.jentz.winter.androidx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import io.jentz.winter.Component
import io.jentz.winter.GFactory

/**
 * Register a [ViewModel] factory on this component.
 *
 * Be careful, view models outlive activities and therefore shouldn't depend on anything that has
 * the same lifetime as an activity. Add this to your applications
 * presentation scope or if you do not maintain one add it to your activity scope and be extra
 * careful with your dependencies.
 */
inline fun <reified R : ViewModel> Component.Builder.viewModel(noinline factory: GFactory<R>) {

    prototype {
        ViewModelProvider(
            instance<ViewModelStore>(),
            WinterViewModelFactory(this, factory)
        ).get(R::class.java)
    }

}
