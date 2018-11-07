package io.jentz.winter

import io.jentz.winter.plugin.PluginRegistry

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
     * The [PluginRegistry] of this application.
     */
    val plugins = PluginRegistry()

    /**
     * The application component.
     */
    var component = emptyComponent()

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
     * Derive the application component and replace it with the derived version.
     *
     * @param qualifier The optional qualifier for the derived component.
     * @param block The component builder block.
     */
    fun derive(qualifier: Any? = null, block: ComponentBuilderBlock) {
        component = component.derive(qualifier, block)
    }

    /**
     * Initialize and return the dependency graph from the configured [component].
     *
     * @param block An optional component builder block to add additional dependencies.
     * @return The new [Graph].
     */
    fun init(block: ComponentBuilderBlock? = null): Graph = component.init(this, block)

}
