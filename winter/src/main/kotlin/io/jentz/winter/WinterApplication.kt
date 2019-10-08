package io.jentz.winter

import io.jentz.winter.plugin.EMPTY_PLUGINS
import io.jentz.winter.plugin.Plugin
import io.jentz.winter.plugin.Plugins

/**
 * [WinterApplication] base class that holds Winter plugins and may be configured with the
 * application [Component].
 *
 * @see Winter
 */
open class WinterApplication() {

    /**
     * Convenient constructor to configure the application [Component] during initialization.
     *
     * Example:
     * ```
     * object MyLibApp : WinterApplication {
     *   // ... declaration of dependencies
     * }
     * ```
     *
     * @param qualifier An optional qualifier for the component.
     * @param block The component builder block.
     */
    constructor(qualifier: Any? = null, block: ComponentBuilderBlock) : this() {
        component(qualifier, block)
    }

    /**
     * The plugins of registered on the application.
     */
    var plugins: Plugins = EMPTY_PLUGINS
        private set

    /**
     * The application component.
     */
    var component = emptyComponent()

    /**
     * If this is set to true, Winter will check for cyclic dependencies and throws an error if it
     * encounters one. Without this check you will run in a StackOverflowError when you accidentally
     * declared a cyclic dependency which may be hard to track down.
     *
     * Cyclic dependency checks are a bit more expensive but usually worth it in debug or test
     * builds.
     *
     */
    var checkForCyclicDependencies: Boolean = false

    /**
     * Register a plugin.
     *
     * Be aware that a plugin is only active for a [Graph] that was created *after* registering
     * it.
     *
     * @param plugin The plugin to register.
     * @return True if plugin was added false otherwise.
     */
    fun registerPlugin(plugin: Plugin): Boolean {
        if (plugins.contains(plugin)) return false
        plugins += plugin
        return true
    }

    /**
     * Unregister a plugin.
     *
     * @param plugin The plugin to register.
     * @return False if the plugin wasn't found otherwise true.
     */
    fun unregisterPlugin(plugin: Plugin): Boolean {
        if (!plugins.contains(plugin)) return false
        plugins -= plugin
        return true
    }

    /**
     * Unregister all plugins.
     */
    fun unregisterAllPlugins() {
        plugins = EMPTY_PLUGINS
    }

    /**
     * Sets the application component by supplying an optional qualifier and a component builder
     * block.
     *
     * @param qualifier The optional qualifier for the new component.
     * @param block The component builder block.
     */
    fun component(qualifier: Any? = null, block: ComponentBuilderBlock) {
        this.component = io.jentz.winter.component(qualifier, block)
    }

    /**
     * Initialize and return the object graph from the application [component].
     *
     * @param block An optional component builder block to add additional dependencies.
     * @return The new [Graph].
     */
    fun createGraph(block: ComponentBuilderBlock? = null): Graph =
        component.createGraph(this, block)

    /**
     * @see createGraph
     */
    @Deprecated(
        "Use createGraph instead.",
        ReplaceWith("createGraph(block)")
    )
    fun init(block: ComponentBuilderBlock? = null): Graph = createGraph(block)

}
