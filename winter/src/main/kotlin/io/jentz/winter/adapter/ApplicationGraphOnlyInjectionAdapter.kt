package io.jentz.winter.adapter

import io.jentz.winter.Graph
import io.jentz.winter.Tree
import io.jentz.winter.WinterApplication

/**
 * Simple adapter for application with only one dependency graph.
 */
open class ApplicationGraphOnlyInjectionAdapter(
    private val tree: Tree
) : WinterApplication.InjectionAdapter {

    override fun get(instance: Any): Graph? = tree.getOrOpen()

}

/**
 * Register an [ApplicationGraphOnlyInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useApplicationGraphOnlyAdapter() {
    injectionAdapter = ApplicationGraphOnlyInjectionAdapter(tree)
}




