package io.jentz.winter.internal

import io.jentz.winter.Graph

interface MembersInjector<in T> {
    fun injectMembers(graph: Graph, target: T)
}