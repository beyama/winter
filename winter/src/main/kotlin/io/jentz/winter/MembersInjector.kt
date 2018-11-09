package io.jentz.winter

interface MembersInjector<in T> {
    fun injectMembers(graph: Graph, target: T)
}
