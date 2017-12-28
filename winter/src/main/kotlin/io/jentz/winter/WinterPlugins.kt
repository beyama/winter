package io.jentz.winter

/**
 * Plugin that is called after an instance was created.
 */
typealias PostConstructPlugin = (graph: Graph, scope: ProviderScope, instance: Any) -> Unit

/**
 * Plugin that is called during an component initialization.
 */
typealias InitializingComponentPlugin = (parentGraph: Graph?, builder: ComponentBuilder) -> Unit

/**
 * Utility class to hook into certain graph lifecycle.
 */
object WinterPlugins {

    private val postConstructPlugins = mutableListOf<PostConstructPlugin>()
    private val initializingComponentPlugins = mutableListOf<InitializingComponentPlugin>()

    internal val hasInitializingComponentPlugins get() = initializingComponentPlugins.isNotEmpty()

    /**
     * Register a [post construct plugin][PostConstructPlugin].
     */
    fun addPostConstructPlugin(plugin: PostConstructPlugin) {
        postConstructPlugins += plugin
    }

    /**
     * Unregister a [post construct plugin][PostConstructPlugin].
     */
    fun removePostConstructPlugin(plugin: PostConstructPlugin) {
        postConstructPlugins -= plugin
    }

    /**
     * Remove all [post construct plugins][PostConstructPlugin].
     */
    fun resetPostConstructPlugins() {
        postConstructPlugins.clear()
    }

    internal fun runPostConstructPlugins(graph: Graph, scope: ProviderScope, instance: Any) {
        postConstructPlugins.forEach { it(graph, scope, instance) }
    }

    /**
     * Register an [initializing component plugin][InitializingComponentPlugin].
     */
    fun addInitializingComponentPlugin(plugin: InitializingComponentPlugin) {
        initializingComponentPlugins += plugin
    }

    /**
     * Unregister an [initializing component plugin][InitializingComponentPlugin]
     */
    fun removeInitializingComponentPlugin(plugin: InitializingComponentPlugin) {
        initializingComponentPlugins -= plugin
    }

    /**
     * Remove all [initializing component plugins][InitializingComponentPlugin].
     */
    fun resetInitializingComponentPlugins() {
        initializingComponentPlugins.clear()
    }

    internal fun runInitializingComponentPlugins(graph: Graph?, builder: ComponentBuilder) {
        initializingComponentPlugins.forEach { it(graph, builder) }
    }

}