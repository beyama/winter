package io.jentz.winter.adapter

import io.jentz.winter.*

/**
 * Simple adapter for application with only one dependency graph.
 */
class ApplicationGraphOnlyAdapter(
    private val tree: WinterTree
) : WinterInjection.Adapter {
    override fun getGraph(instance: Any): Graph = tree.get()

    override fun createGraph(
        instance: Any,
        builderBlock: ComponentBuilderBlock?
    ): Graph = tree.open(block = builderBlock)

    override fun disposeGraph(instance: Any) {
        tree.close()
    }
}

/**
 * Register an [ApplicationGraphOnlyAdapter] on this [WinterInjection] instance.
 *
 * Use the [tree] parameter if you have your own object version of [WinterTree] that should be used
 * which may be useful when Winter is used in a library.
 *
 * @param tree The tree to operate on.
 */
fun WinterInjection.useApplicationGraphOnlyAdapter(tree: WinterTree = GraphRegistry) {
    adapter = ApplicationGraphOnlyAdapter(tree)
}

/**
 * Register an [ApplicationGraphOnlyAdapter] on this [WinterInjection] instance.
 *
 * @param application The [WinterApplication] instance to be used by the adapter.
 */
fun WinterInjection.useApplicationGraphOnlyAdapter(application: WinterApplication) {
    adapter = ApplicationGraphOnlyAdapter(WinterTree(application))
}




