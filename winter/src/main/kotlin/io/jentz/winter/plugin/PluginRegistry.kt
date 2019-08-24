package io.jentz.winter.plugin

import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Graph
import io.jentz.winter.Scope
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The registry for [Winter plugins][Plugin].
 */
class PluginRegistry {
    private val plugins = CopyOnWriteArrayList<Plugin>()

    /**
     * The number of registered plugins.
     */
    val size: Int get() = plugins.size

    /**
     * Register a plugin.
     *
     * The plugin will only be registered if the registry doesn't already contain the plugin.
     *
     * @param plugin The plugin to register.
     * @return True if plugin was added false otherwise.
     */
    fun register(plugin: Plugin): Boolean {
        if (contains(plugin)) return false
        plugins.add(plugin)
        return true
    }

    /**
     * Unregister a plugin.
     *
     * @param plugin The plugin to register.
     * @return False if the plugin wasn't found otherwise true.
     */
    fun unregister(plugin: Plugin): Boolean = plugins.remove(plugin)

    /**
     * Unregister all plugins.
     */
    fun unregisterAll() {
        plugins.clear()
    }

    /**
     * Returns true if the plugin is already registered.
     *
     * @param plugin The plugin to check for.
     * @return true if the registry contains the plugin
     */
    fun contains(plugin: Plugin): Boolean = plugins.contains(plugin)

    /**
     * Returns true if the registry contains no plugin.
     */
    fun isEmpty(): Boolean = plugins.isEmpty()

    /**
     * Returns true if the registry contains plugins.
     */
    fun isNotEmpty(): Boolean = plugins.isNotEmpty()

    internal fun runGraphInitializing(parentGraph: Graph?, builder: ComponentBuilder) {
        plugins.forEach { it.graphInitializing(parentGraph, builder) }
    }

    internal fun runGraphInitialized(graph: Graph) {
        plugins.forEach { it.graphInitialized(graph) }
    }

    internal fun runGraphDispose(graph: Graph) {
        plugins.forEach { it.graphDispose(graph) }
    }

    internal fun runPostConstruct(graph: Graph, scope: Scope, argument: Any, instance: Any) {
        plugins.forEach { it.postConstruct(graph, scope, argument, instance) }
    }

}
