package io.jentz.winter

import io.jentz.winter.internal.*

/**
 * Component builder DSL.
 */
class ComponentBuilder internal constructor() {
    private val registry: MutableMap<DependencyKey, ComponentEntry<*>> = mutableMapOf()

    /**
     * Include dependency from the given component into the new component.
     *
     * @param component The component to include the dependency provider from.
     */
    fun include(component: Component) {
        component.dependencyMap.forEach { k, v -> register(k, v, false) }
    }

    /**
     * Register a provider for instances of type [T].
     *
     * @param qualifier An optional qualifier.
     * @param scope A [ProviderScope] like [singleton].
     * @param generics If true this will preserve generic information of [T].
     * @param override If true this will override a existing provider of this type.
     * @param block The provider block.
     */
    inline fun <reified T : Any> provider(qualifier: Any? = null,
                                          scope: ProviderScope = prototype,
                                          generics: Boolean = false,
                                          override: Boolean = false,
                                          noinline block: Graph.() -> T) {
        val key = if (generics) genericTypeKey<T>(qualifier) else typeKey<T>(qualifier)
        register(key, ProviderEntry(scope, block), override)
    }

    /**
     * Register a singleton scoped provider for an instance of type [T].
     *
     * This is syntactic sugar for [provider] with parameter scope = [singleton].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [T].
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
     * Register a factory that takes [A] and returns [R].
     *
     * @param qualifier An optional qualifier.
     * @param scope A [FactoryScope] like [multiton].
     * @param generics If true this will preserve generic information of [A] and [R].
     * @param override If true this will override a existing factory of this type.
     * @param block The factory block.
     */
    inline fun <reified A : Any, reified R : Any> factory(qualifier: Any? = null,
                                                          scope: FactoryScope = prototypeFactory,
                                                          generics: Boolean = false,
                                                          override: Boolean = false,
                                                          noinline block: Graph.(A) -> R) {
        val key = if (generics) genericCompoundTypeKey<A, R>(qualifier) else compoundTypeKey<A, R>(qualifier)
        register(key, FactoryEntry(scope, block), override)
    }

    /**
     * Register a constant of type [T].
     *
     * @param value The value of this constant provider.
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [T].
     * @param override If true this will override a existing provider of this type.
     */
    inline fun <reified T : Any> constant(value: T,
                                          qualifier: Any? = null,
                                          generics: Boolean = false,
                                          override: Boolean = false) {
        val key = if (generics) genericTypeKey<T>(qualifier) else typeKey<T>(qualifier)
        register(key, ConstantEntry(value), override)
    }

    /**
     * Register a members injector for [T].
     */
    inline fun <reified T : Any> membersInjector(noinline block: () -> MembersInjector<T>) {
        register(membersInjectorKey<T>(), MembersInjectorEntry(block), false)
    }

    /**
     * Remove a provider of type [T].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [T].
     * @param silent If true this will not throw an exception if the provider doesn't exist.
     */
    inline fun <reified T : Any> removeProvider(qualifier: Any? = null,
                                                generics: Boolean = false,
                                                silent: Boolean = false) {
        val key = if (generics) genericTypeKey<T>(qualifier) else typeKey<T>(qualifier)
        remove(key, silent)
    }

    /**
     * Remove a factory of type `([A]) -> [R]`.
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [A] and [R].
     * @param silent If true this will not throw an exception if the factory doesn't exist.
     */
    inline fun <reified A : Any, reified R : Any> removeFactory(qualifier: Any? = null,
                                                                generics: Boolean = false,
                                                                silent: Boolean = false) {
        val key = if (generics) genericCompoundTypeKey<A, R>(qualifier) else compoundTypeKey<A, R>(qualifier)
        remove(key, silent)
    }

    /**
     * Register a subcomponent.
     *
     * @param qualifier The qualifier of the subcomponent.
     * @param override If true an existing subcomponent will be replaced.
     * @param deriveExisting If true an existing subcomponent will be derived and replaced with the derived version.
     * @param block A builder block to register provider on the subcomponent.
     */
    fun subcomponent(qualifier: Any,
                     override: Boolean = false,
                     deriveExisting: Boolean = false,
                     block: ComponentBuilder.() -> Unit) {
        if (override && deriveExisting) {
            throw WinterException("You can either override existing or derive existing but not both.")
        }

        val key = typeKey<Component>(qualifier)

        val existingEntry = registry[key] as? ConstantEntry<*>

        if (existingEntry != null && !(override || deriveExisting)) {
            throw WinterException("Subcomponent with qualifier `$qualifier` already exists.")
        }

        if (existingEntry == null && override) {
            throw WinterException("Subcomponent with qualifier `$qualifier` doesn't exist but override is true.")
        }

        val component = if (deriveExisting) {
            if (existingEntry == null) {
                throw WinterException("Subcomponent with qualifier `$qualifier` doesn't exist but deriveExisting is true.")
            }
            val baseComponent = existingEntry.value as Component
            baseComponent.derive(block)
        } else {
            ComponentBuilder().apply { this.block() }.build()
        }

        constant(qualifier = qualifier, override = (override || deriveExisting), value = component)
    }

    /**
     * Register a [ComponentEntry] by [DependencyKey].
     *
     * @suppress
     */
    fun register(key: DependencyKey, entry: ComponentEntry<*>, override: Boolean) {
        val alreadyExists = registry.containsKey(key)

        if (alreadyExists && !override) {
            throw WinterException("Entry with key `$key` already exists.")
        }

        if (!alreadyExists && override) {
            throw WinterException("Entry with key `$key` doesn't exist but override is true.")
        }

        registry[key] = entry
    }

    /**
     * Remove a [ComponentEntry] by [DependencyKey].
     *
     * @suppress
     */
    fun remove(key: DependencyKey, silent: Boolean) {
        if (!silent && !registry.containsKey(key)) {
            throw WinterException("Can't remove entry with key `$key` because it doesn't exist.")
        }
        registry.remove(key)
    }

    internal fun build() = Component(registry)
}