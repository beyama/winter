package io.jentz.winter.androidx.fragment

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.jentz.winter.Component

internal fun exportAndroidTypes(
    instance: Any,
    enableWinterFragmentFactory: Boolean,
    builder: Component.Builder
) {
    with(builder) {
        if (instance is FragmentActivity) {
            constant(instance)
            constant(instance.supportFragmentManager)

            if (enableWinterFragmentFactory) {
                eagerSingleton(
                    onPostConstruct = { instance<FragmentManager>().fragmentFactory = it }
                ) { WinterFragmentFactory(this) }
            }
        }
    }
}
