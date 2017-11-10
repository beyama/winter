package io.jentz.winter

import io.jentz.winter.internal.*

/**
 * Component builder DSL.
 */
class ComponentBuilder internal constructor() {
    private val registry: MutableMap<DependencyKey, ComponentEntry<*>> = mutableMapOf()
    private var subcomponentBuilders: MutableMap<DependencyKey, ComponentBuilder>? = null

    enum class SubcomponentIncludeMode {
        /**
         * Do not include subcomponents from the component to include.
         */
        DoNotInclude,
        /**
         * Do not include a subcomponent with a qualifier that is already present in the including component.
         */
        DoNotIncludeIfAlreadyPresent,
        /**
         * Replace an existing subcomponent with same qualifier if already present in the including component.
         */
        Replace,
        /**
         * If a component with the same qualifier already exists in the including component then derive from it
         * and include the subcomponent with same qualifier from the component to include.
         */
        Merge
    }

    /**
     * Include dependency from the given component into the new component.
     *
     * @param component The component to include the dependency provider from.
     */
    fun include(component: Component,
                override: Boolean = true,
                subcomponentIncludeMode: SubcomponentIncludeMode = SubcomponentIncludeMode.Merge) {
        component.dependencyMap.forEach { k, v ->
            if (v is ConstantEntry<*> && v.value is Component) {
                @Suppress("UNCHECKED_CAST")
                registerSubcomponent(k, v as ConstantEntry<Component>, subcomponentIncludeMode)
            } else {
                if (!override && registry.containsKey(k)) {
                    throw WinterException("Entry with key `$k` already exists.")
                }
                registry[k] = v
            }
        }
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

        val doesAlreadyExist = registry.containsKey(key) || subcomponentBuilders?.containsKey(key) == true

        if (doesAlreadyExist && !(override || deriveExisting)) {
            throw WinterException("Subcomponent with qualifier `$qualifier` already exists.")
        }

        if (!doesAlreadyExist && override) {
            throw WinterException("Subcomponent with qualifier `$qualifier` doesn't exist but override is true.")
        }

        if (!doesAlreadyExist && deriveExisting) {
            throw WinterException("Subcomponent with qualifier `$qualifier` doesn't exist but deriveExisting is true.")
        }

        getOrCreateSubcomponentBuilder(key).also(block)
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

    private fun registerSubcomponent(key: DependencyKey,
                                     entry: ConstantEntry<Component>,
                                     subcomponentIncludeMode: SubcomponentIncludeMode) {
        when (subcomponentIncludeMode) {
            SubcomponentIncludeMode.DoNotInclude -> {
            }
            SubcomponentIncludeMode.DoNotIncludeIfAlreadyPresent -> {
                if (!registry.containsKey(key) && subcomponentBuilders?.containsKey(key) != true) {
                    registry[key] = entry
                }
            }
            SubcomponentIncludeMode.Replace -> {
                subcomponentBuilders?.remove(key)
                registry[key] = entry
            }
            SubcomponentIncludeMode.Merge -> {
                val builder = getOrCreateSubcomponentBuilder(key)
                builder.include(entry.value)
            }
        }
    }

    private fun getOrCreateSubcomponentBuilder(key: DependencyKey): ComponentBuilder {
        subcomponentBuilders?.get(key)?.let { return it }

        val builders = subcomponentBuilders ?: mutableMapOf<DependencyKey, ComponentBuilder>().also { subcomponentBuilders = it }
        val constant = registry.remove(key) as? ConstantEntry<*>
        val existingSubcomponent = constant?.value as? Component

        return ComponentBuilder().also { builder ->
            existingSubcomponent?.let { builder.include(it) }
            builders[key] = builder
        }
    }

    internal fun build(): Component {
        val dependencyMap = DependencyMap<ComponentEntry<*>>(registry.size + (subcomponentBuilders?.size ?: 0))
        registry.forEach { (k, v) -> dependencyMap[k] = v }
        subcomponentBuilders?.forEach { (k, v) -> dependencyMap[k] = ConstantEntry(v.build()) }
        return Component(dependencyMap)
    }
}