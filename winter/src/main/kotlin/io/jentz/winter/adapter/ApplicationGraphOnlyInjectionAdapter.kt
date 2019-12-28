package io.jentz.winter.adapter

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.Tree
import io.jentz.winter.WinterApplication

/**
 * Simple adapter for application with only one dependency graph.
 */
class ApplicationGraphOnlyInjectionAdapter internal constructor(
    private val tree: Tree
) : WinterApplication.InjectionAdapter {
    override fun get(instance: Any): Graph? = tree.getOrNull()

    override fun open(
        instance: Any,
        block: ComponentBuilderBlock?
    ): Graph = tree.open(block = block)

    override fun close(instance: Any) {
        tree.close()
    }
}

/**
 * Register an [ApplicationGraphOnlyInjectionAdapter] on this [WinterApplication] instance.
 */
fun WinterApplication.useApplicationGraphOnlyAdapter() {
    injectionAdapter = ApplicationGraphOnlyInjectionAdapter(tree)
}




