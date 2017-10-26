package io.jentz.winter

import io.jentz.winter.internal.*

/**
 * Component builder DSL.
 */
class ComponentBuilder internal constructor() {
    private val registry: MutableMap<DependencyId, ComponentEntry> = mutableMapOf()

    /**
     * Include dependency from the given component into the new component.
     *
     * @param component The component to include the dependency provider from.
     */
    fun include(component: Component) {
        component.dependencyMap.forEach { k, v -> register(k, v, false) }
    }

    /**
     * Register a provider for instances of type `T`.
     *
     * @param qualifier An optional qualifier.
     * @param scope A [ProviderScope] like [singleton].
     * @param generics If true this will preserve generic information of `T`.
     * @param override If true this will override a existing provider of this type.
     * @param block The provider block.
     */
    inline fun <reified T : Any> provider(qualifier: Any? = null,
                                          scope: ProviderScope = prototype,
                                          generics: Boolean = false,
                                          override: Boolean = false,
                                          noinline block: Graph.() -> T) {
        val id = if (generics) genericProviderId<T>(qualifier) else providerId<T>(qualifier)
        register(id, Provider(scope, block), override)
    }

    /**
     * Register a singleton scoped provider for an instance of type `T`.
     *
     * This is syntactic sugar for [provider] with parameter scope is [singleton].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of `T`.
     * @param override If true this will override a existing provider of this type.
     * @param block The provider block.
     */
    inline fun <reified T : Any> singleton(qualifier: Any? = null,
                                           generics: Boolean = false,
                                           override: Boolean = false,
                                           noinline block: Graph.() -> T) {
        provider(qualifier, singleton, generics, override, block)
    }


    /**
     * Register a factory that takes `A` and returns `R`.
     *
     * @param qualifier An optional qualifier.
     * @param scope A [FactoryScope] like [multiton].
     * @param generics If true this will preserve generic information of `A` and `R`.
     * @param override If true this will override a existing factory of this type.
     * @param block The factory block.
     */
    inline fun <reified A : Any, reified R : Any> factory(qualifier: Any? = null,
                                                          scope: FactoryScope = prototypeFactory,
                                                          generics: Boolean = false,
                                                          override: Boolean = false,
                                                          noinline block: Graph.(A) -> R) {
        val id = if (generics) genericFactoryId<A, R>(qualifier) else factoryId<A, R>(qualifier)
        register(id, Factory(scope, block), override)
    }

    /**
     * Register a constant of type `T`.
     *
     * @param value The value of this constant provider.
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of `T`.
     * @param override If true this will override a existing provider of this type.
     */
    inline fun <reified T : Any> constant(value: T,
                                          qualifier: Any? = null,
                                          generics: Boolean = false,
                                          override: Boolean = false) {
        val id = if (generics) genericProviderId<T>(qualifier) else providerId<T>(qualifier)
        register(id, Constant(value), override)
    }

    /**
     * Remove a provider of type `T`.
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of `T`.
     * @param silent If true this will not throw an exception if the provider doesn't exist.
     */
    inline fun <reified T : Any> removeProvider(qualifier: Any? = null,
                                                generics: Boolean = false,
                                                silent: Boolean = false) {
        val id = if (generics) genericProviderId<T>(qualifier) else providerId<T>(qualifier)
        remove(id, silent)
    }

    /**
     * Remove a factory of type `(A) -> R`.
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of `A` and `R`.
     * @param silent If true this will not throw an exception if the factory doesn't exist.
     */
    inline fun <reified A : Any, reified R : Any> removeFactory(qualifier: Any? = null,
                                                                generics: Boolean = false,
                                                                silent: Boolean = false) {
        val id = if (generics) genericFactoryId<A, R>(qualifier) else factoryId<A, R>(qualifier)
        remove(id, silent)
    }

    /**
     * Register a subcomponent.
     *
     * @param name The name of the subcomponent.
     * @param override If true an existing subcomponent will be replaced.
     * @param deriveExisting If true an existing subcomponent will be derived and replaced with the derived version.
     * @param block A builder block to register provider on the subcomponent.
     */
    fun subcomponent(name: String,
                     override: Boolean = false,
                     deriveExisting: Boolean = false,
                     block: ComponentBuilder.() -> Unit) {
        if (override && deriveExisting) {
            throw WinterException("You can either override existing or derive existing but not both.")
        }

        val id = providerId<Component>(name)

        val existingEntry = registry[id] as? Constant<*>

        if (existingEntry != null && !(override || deriveExisting)) {
            throw WinterException("Subcomponent with name `$name` already exists.")
        }

        if (existingEntry == null && override) {
            throw WinterException("Subcomponent with name `$name` doesn't exist but override is true.")
        }

        val component = if (deriveExisting) {
            if (existingEntry == null) {
                throw WinterException("Subcomponent with name `$name` doesn't exist but deriveExisting is true.")
            }
            val baseComponent = existingEntry.value as Component
            baseComponent.derive(block)
        } else {
            ComponentBuilder().apply { this.block() }.build()
        }

        constant(qualifier = name, override = (override || deriveExisting), value = component)
    }

    /**
     * Register a [ComponentEntry] by [DependencyId].
     *
     * @suppress
     */
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

    /**
     * Remove a [ComponentEntry] by [DependencyId].
     *
     * @suppress
     */
    fun remove(id: DependencyId, silent: Boolean) {
        if (!silent && !registry.containsKey(id)) {
            throw WinterException("Can't remove entry with `$id` because it doesn't exist.")
        }
        registry.remove(id)
    }

    internal fun build() = Component(registry)
}