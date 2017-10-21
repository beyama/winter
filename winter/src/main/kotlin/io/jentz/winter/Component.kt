package io.jentz.winter

import io.jentz.winter.internal.ComponentEntry
import io.jentz.winter.internal.DependencyId
import io.jentz.winter.internal.DependencyMap

fun component(block: ComponentBuilder.() -> Unit) = ComponentBuilder().apply { this.block() }.build()

class Component internal constructor(registry: Map<DependencyId, ComponentEntry>) {

    internal val dependencyMap: DependencyMap<ComponentEntry> = DependencyMap(registry)

    fun derive(block: ComponentBuilder.() -> Unit) = component {
        include(this@Component)
        block()
    }

    fun init(block: (ComponentBuilder.() -> Unit)? = null): Graph {
        val component = if (block != null) derive(block) else this
        return Graph(null, component)
    }

}