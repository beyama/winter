package io.jentz.winter

import io.jentz.winter.plugin.PluginRegistry

open class WinterApplication() {

    constructor(qualifier: Any? = null, block: ComponentBuilderBlock) : this() {
        component(qualifier, block)
    }

    val plugins = PluginRegistry()

    private var _component: Component? = null

    val component: Component
        get() = _component ?: throw WinterException("component is not configured")

    fun component(qualifier: Any? = null, block: ComponentBuilderBlock) {
        _component = io.jentz.winter.component(qualifier, block)
    }

    fun init(block: ComponentBuilderBlock? = null): Graph = component.init(this, block)

}

