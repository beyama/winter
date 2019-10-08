package io.jentz.winter.plugin

import io.jentz.winter.ComponentBuilder
import io.jentz.winter.Graph
import io.jentz.winter.Scope

internal val EMPTY_PLUGINS = Plugins()

/**
 * Container for [Winter plugins][Plugin].
 */
class Plugins private constructor(
    private val list: List<Plugin>
): Iterable<Plugin> {

    constructor() : this(emptyList())

    constructor(vararg plugins: Plugin) : this(plugins.toList())

    /**
     * The number of registered list.
     */
    val size: Int get() = list.size

    /**
     * Returns true if the plugin is already registered.
     *
     * @param plugin The plugin to check for.
     * @return true if the registry contains the plugin
     */
    fun contains(plugin: Plugin): Boolean = list.contains(plugin)

    /**
     * Returns true if the registry contains no plugin.
     */
    fun isEmpty(): Boolean = list.isEmpty()

    /**
     * Returns true if the registry contains list.
     */
    fun isNotEmpty(): Boolean = list.isNotEmpty()

    internal operator fun plus(plugin: Plugin): Plugins =
        if (contains(plugin)) this else Plugins(list + plugin)

    internal operator fun minus(plugin: Plugin): Plugins =
        if (contains(plugin)) Plugins(list - plugin) else this

    override fun iterator(): Iterator<Plugin> = list.iterator()

    internal fun runGraphInitializing(parentGraph: Graph?, builder: ComponentBuilder) {
        forEach { it.graphInitializing(parentGraph, builder) }
    }

    internal fun runGraphInitialized(graph: Graph) {
        forEach { it.graphInitialized(graph) }
    }

    internal fun runGraphDispose(graph: Graph) {
        forEach { it.graphDispose(graph) }
    }

    internal fun runPostConstruct(graph: Graph, scope: Scope, argument: Any, instance: Any) {
        forEach { it.postConstruct(graph, scope, argument, instance) }
    }

}
