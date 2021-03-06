package io.jentz.winter.inject

import io.jentz.winter.Graph

/**
 * Interface for members injectors generated by Winter compiler.
 */
interface MembersInjector<T : Any> {
    fun inject(graph: Graph, target: T)
}
