package io.jentz.winter

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Plugin that is called after an instance was created.
 */
typealias PostConstructPlugin = (graph: Graph, scope: Scope, instance: Any) -> Unit

/**
 * Plugin that is called during an component initialization.
 */
typealias InitializingComponentPlugin = (parentGraph: Graph?, builder: ComponentBuilder) -> Unit

/**
 * Plugin that is called on [Graph.dispose].
 */
typealias GraphDisposePlugin = (graph: Graph) -> Unit

/**
 * Utility class to hook into certain graph lifecycle.
 */
object WinterPlugins {
    private val postConstructPlugins = CopyOnWriteArrayList<PostConstructPlugin>()
    private val initializingComponentPlugins = CopyOnWriteArrayList<InitializingComponentPlugin>()
    private val graphDisposePlugins = CopyOnWriteArrayList<GraphDisposePlugin>()

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

    internal fun runPostConstructPlugins(graph: Graph, scope: Scope, instance: Any) {
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

    /**
     * Register a [graph dispose plugin][GraphDisposePlugin].
     */
    fun addGraphDisposePlugin(plugin: GraphDisposePlugin) {
        graphDisposePlugins += plugin
    }

    /**
     * Unregister a [graph dispose plugin][GraphDisposePlugin].
     */
    fun removeGraphDisposePlugin(plugin: GraphDisposePlugin) {
        graphDisposePlugins -= plugin
    }

    /**
     * Remove all [graph dispose plugins][GraphDisposePlugin].
     */
    fun resetGraphDisposePlugins() {
        graphDisposePlugins.clear()
    }

    internal fun runGraphDisposePlugins(graph: Graph) {
        graphDisposePlugins.forEach { it(graph) }
    }

}