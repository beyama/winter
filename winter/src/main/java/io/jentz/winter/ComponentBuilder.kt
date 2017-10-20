package io.jentz.winter

import io.jentz.winter.internal.*

class ComponentBuilder internal constructor() {
    private val registry: MutableMap<DependencyId, ComponentEntry> = mutableMapOf()

    fun include(component: Component) {
        component.dependencyMap.forEach { k, v -> register(k, v, false) }
    }

    inline fun <reified T : Any> provider(qualifier: Any? = null,
                                          scope: ProviderScope = prototype,
                                          generics: Boolean = false,
                                          override: Boolean = false,
                                          noinline block: Graph.() -> T) {
        val id = if (generics) genericProviderId<T>(qualifier) else providerId<T>(qualifier)
        register(id, Provider(scope, block), override)
    }

    inline fun <reified A : Any, reified R : Any> factory(qualifier: Any? = null,
                                                          scope: FactoryScope = prototypeFactory,
                                                          generics: Boolean = false,
                                                          override: Boolean = false,
                                                          noinline block: Graph.(A) -> R) {
        val id = if (generics) genericFactoryId<A, R>(qualifier) else factoryId<A, R>(qualifier)
        register(id, Factory(scope, block), override)
    }

    inline fun <reified T : Any> constant(value: T,
                                          qualifier: Any? = null,
                                          generics: Boolean = false,
                                          override: Boolean = false) {
        val id = if (generics) genericProviderId<T>(qualifier) else providerId<T>(qualifier)
        register(id, Constant(value), override)
    }

    fun subComponent(name: String,
                     override: Boolean = false,
                     deriveExisting: Boolean = false,
                     block: ComponentBuilder.() -> Unit) {
        if (override && deriveExisting) {
            throw WinterException("You can either override existing or derive existing but not both.")
        }

        val id = providerId<Constant<*>>(name)

        val existingEntry = registry[id] as? Constant<*>

        if (existingEntry != null && !(override || deriveExisting)) {
            throw WinterException("Sub-component with name `$name` already exists.")
        }

        if (existingEntry == null && override) {
            throw WinterException("Sub-component with name `$name` doesn't exist but override is true.")
        }

        val component = if (deriveExisting) {
            if (existingEntry == null) {
                throw WinterException("Sub-component with name `$name` doesn't exist but deriveExisting is true.")
            }
            val baseComponent = existingEntry.value as Component
            baseComponent.derive(block)
        } else {
            ComponentBuilder().apply { this.block() }.build()
        }

        constant(qualifier = name, override = (override || deriveExisting), value = component)
    }

    fun register(id: DependencyId, entry: ComponentEntry, override: Boolean) {
        val alreadyExists = registry.containsKey(id)

        if (alreadyExists && !override) {
            throw WinterException("Entry with ID `$id` already exists.")
        }

        if (!alreadyExists && override) {
            throw WinterException("Entry with ID `$id` doesn't exist but override is true.")
        }

        registry[id] = entry
    }

    fun build() = Component(registry)

}