package io.jentz.winter.androidx.dsl

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.jentz.winter.ApplicationScope
import io.jentz.winter.GCallback
import io.jentz.winter.services.SingletonService
import io.jentz.winter.typeKey

/**
 * Registers an Android [androidx.lifecycle.LifecycleObserver] on the [ProcessLifecycleOwner] and
 * calls the provided callbacks accordingly with the [io.jentz.winter.Graph] as receiver and [R] as
 * argument.
 *
 * @param onCreate Called on [Lifecycle.Event.ON_CREATE]
 * @param onStart Called on [Lifecycle.Event.ON_START]
 * @param onResume Called on [Lifecycle.Event.ON_RESUME]
 * @param onPause Called on [Lifecycle.Event.ON_PAUSE]
 * @param onStop Called on [Lifecycle.Event.ON_STOP]
 */
inline fun <reified R: Any> SingletonService<R>.androidProcessLifecycle(
    noinline onCreate: GCallback<R>? = null,
    noinline onStart: GCallback<R>? = null,
    noinline onResume: GCallback<R>? = null,
    noinline onPause: GCallback<R>? = null,
    noinline onStop: GCallback<R>? = null
) = addSideEffect { instance ->
    val observer = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            onCreate?.let { it(instance) }
        }

        override fun onStart(owner: LifecycleOwner) {
            onStart?.let { it(instance) }
        }

        override fun onResume(owner: LifecycleOwner) {
            onResume?.let { it(instance) }
        }

        override fun onPause(owner: LifecycleOwner) {
            onPause?.let { it(instance) }
        }

        override fun onStop(owner: LifecycleOwner) {
            onStop?.let { it(instance) }
        }
    }

    val lifecycle: Lifecycle = instance(typeKey(ApplicationScope))
    lifecycle.addObserver(observer)

    return@addSideEffect { lifecycle.removeObserver(observer) }
}