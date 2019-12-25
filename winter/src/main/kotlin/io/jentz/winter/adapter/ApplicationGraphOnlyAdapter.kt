package io.jentz.winter.adapter

import io.jentz.winter.*

/**
 * Simple adapter for application with only one dependency graph.
 */
class ApplicationGraphOnlyAdapter(
    private val app: WinterApplication
) : WinterInjection.Adapter {
    override fun getGraph(instance: Any): Graph = app.get()

    override fun createGraph(
        instance: Any,
        block: ComponentBuilderBlock?
    ): Graph = app.open(block = block)

    override fun disposeGraph(instance: Any) {
        app.close()
    }
}

/**
 * Register an [ApplicationGraphOnlyAdapter] on this [WinterInjection] instance.
 *
 * @param application The [WinterApplication] instance to be used by the adapter.
 */
fun WinterInjection.useApplicationGraphOnlyAdapter(application: WinterApplication = Winter) {
    adapter = ApplicationGraphOnlyAdapter(application)
}




