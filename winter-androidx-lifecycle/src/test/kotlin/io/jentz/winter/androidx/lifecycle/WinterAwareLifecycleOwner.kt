package io.jentz.winter.androidx.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.nhaarman.mockitokotlin2.mock
import io.jentz.winter.WinterApplication
import io.jentz.winter.adapter.useApplicationGraphOnlyAdapter
import io.jentz.winter.aware.WinterAware

class WinterAwareLifecycleOwner : WinterAware, LifecycleOwner {
    override val winterApplication: WinterApplication = WinterApplication().apply {
        component {}
        useApplicationGraphOnlyAdapter()
        tree.open()
    }

    val registry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = registry

    init {
        registry.markState(Lifecycle.State.RESUMED)
    }
}