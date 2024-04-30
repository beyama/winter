package io.jentz.winter.androidx.dsl

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.jentz.winter.ApplicationScope
import io.jentz.winter.Component
import io.jentz.winter.erased

inline fun <reified A: Application> Component.Builder.application(application: A) {
    constant<A>(application)
    constant<Context>(application)
    if (!containsKey(erased<Application>())) { // In case A is just android.app.Application
        constant<Application>(application)
    }
    constant(ProcessLifecycleOwner.get().lifecycle, erased(ApplicationScope))
    constant(ProcessLifecycleOwner.get().lifecycleScope, erased(ApplicationScope))
}