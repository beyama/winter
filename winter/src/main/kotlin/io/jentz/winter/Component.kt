package io.jentz.winter

import io.jentz.winter.internal.ComponentEntry
import io.jentz.winter.internal.ConstantEntry

/**
 * The Component stores the dependency providers which are than retrieved and instantiated by an instance of a
 * [graph][Graph].
 *
 * Instances are build by calling [component] with a [builder][ComponentBuilder] block.
 *
 * Components are immutable but an extended variant can be created by calling [derive] with a builder block.
 *
 * ```
 * val appComponent = component {
 *     provider<MyService>(scope = singleton) { MyServiceImpl(instance()) }
 * }
 * val derived = appComponent.derive {
 *     provider<MyOtherService> { MyOtherServiceImpl(instance(), instance("named")) }
 * }
 * val graph = derived.init { constant<Application>(myAndroidApplication) }
 * ```
 */
class Component internal constructor(
        /**
         * The components qualifier (null for root components and the sub-components qualifier for sub-components).
         */
        val qualifier: Any?,
        internal val dependencies: Map<DependencyKey, ComponentEntry<*>>
) {

    /**
     * Create an extended copy of this component.
     *
     * @param qualifier An optional qualifier (default: [qualifier]).
     * @param block A builder block that is called in the context of a [ComponentBuilder].
     * @return A new [Component] that contains all provider of the base component plus the one defined in the builder block.
     */
    fun derive(qualifier: Any? = this.qualifier, block: ComponentBuilderBlock) = component(qualifier) {
        include(this@Component)
        block()
    }

    /**
     * Returns a subcomponent by its qualifier or a nested subcomponent by its path of qualifiers.
     *
     * Main usage for this is to restructure components when using [ComponentBuilder.include] in conjunction
     * with [ComponentBuilder.SubcomponentIncludeMode.DoNotInclude].
     *
     * @param qualifiers The qualifier/path of qualifiers of the subcomponent
     * @return The subcomponent
     *
     * @throws EntryNotFoundException If the component does not exist.
     */
    fun subcomponent(vararg qualifiers: Any): Component {
        var component: Component = this
        qualifiers.forEach { qualifier ->
            val constant = component.dependencies[typeKey<Component>(qualifier)] as? ConstantEntry<*>
            if (constant == null) {
                val path = qualifiers.joinToString(", ")
                throw EntryNotFoundException("Subcomponent with path [$path] doesn't exist.")
            }
            component = constant.value as Component
        }
        return component
    }

    /**
     * Create a [dependency graph][Graph] from this component.
     *
     * @param block An optional builder block to extend the component before creating the graph.
     * @return An instance of [Graph] backed by this component.
     */
    fun init(block: ComponentBuilderBlock? = null): Graph {
        return initializeGraph(null, this, block)
    }

}