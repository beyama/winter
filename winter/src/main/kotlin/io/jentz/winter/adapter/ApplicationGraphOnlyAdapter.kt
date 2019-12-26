package io.jentz.winter.adapter

import io.jentz.winter.*

/**
 * Simple adapter for application with only one dependency graph.
 */
class ApplicationGraphOnlyAdapter(
    private val tree: Tree
) : WinterInjection.Adapter {
    override fun getGraph(instance: Any): Graph = tree.get()

    override fun createGraph(
        instance: Any,
        block: ComponentBuilderBlock?
    ): Graph = tree.open(block = block)

    override fun disposeGraph(instance: Any) {
        tree.close()
    }
}

/**
 * Register an [ApplicationGraphOnlyAdapter] on this [WinterInjection] instance.
 *
 * @param application The [WinterApplication] instance to be used by the adapter.
 */
fun WinterInjection.useApplicationGraphOnlyAdapter(application: WinterApplication = Winter) {
    adapter = ApplicationGraphOnlyAdapter(application.tree)
}




