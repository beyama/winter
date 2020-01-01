package io.jentz.winter.inject

import io.jentz.winter.Component
import io.jentz.winter.Graph

/**
 * Interface implemented by factories generated by winter-compiler.
 */
interface Factory<R> : (Graph) -> R {

    /**
     * Register the factory on the given [builder] instance.
     */
    fun register(builder: Component.Builder)

}
