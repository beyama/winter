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

    @Suppress("UNCHECKED_CAST")
    var eagerDependencies: Set<DependencyKey>
        get() = (registry[eagerDependenciesKey] as? ConstantEntry<Set<DependencyKey>>)
                ?.let { return it.value }
                ?: emptySet()
        set(value) {
            registry[eagerDependenciesKey] = ConstantEntry(value)
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
            when {
                k === eagerDependenciesKey -> {
                    @Suppress("UNCHECKED_CAST")
                    val entry = v as ConstantEntry<Set<DependencyKey>>
                    eagerDependencies += entry.value
                }
                v is ConstantEntry<*> && v.value is Component -> {
                    @Suppress("UNCHECKED_CAST")
                    registerSubcomponent(k, v as ConstantEntry<Component>, subcomponentIncludeMode)
                }
                else -> {
                    if (!override && registry.containsKey(k)) {
                        throw WinterException("Entry with key `$k` already exists.")
                    }
                    registry[k] = v
                }
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
                                          noinline block: ProviderBlock<T>) {
        registerProvider(providerKey<T>(qualifier, generics), scope, override, block)
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
                                           noinline block: ProviderBlock<T>) {
        registerProvider(providerKey<T>(qualifier, generics), singleton, override, block)
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
    inline fun <reified T : Any> eagerSingleton(qualifier: Any? = null,
                                                generics: Boolean = false,
                                                override: Boolean = false,
                                                noinline block: ProviderBlock<T>) {
        val key = providerKey<T>(qualifier, generics)
        registerProvider(key, singleton, override, block)
        eagerDependencies += key
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
                                                          noinline block: FactoryBlock<A, R>) {
        registerFactory(factoryKey<A, R>(qualifier, generics), scope, override, block)
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
        registerConstant(providerKey<T>(qualifier, generics), override, value)
    }

    /**
     * Register a members injector for [T].
     */
    inline fun <reified T : Any> membersInjector(noinline block: () -> MembersInjector<T>) {
        registerMembersInjector(membersInjectorKey<T>(), block)
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
     * Create [DependencyKey] for provider.
     */
    inline fun <reified T : Any> providerKey(qualifier: Any? = null, generics: Boolean = false): DependencyKey =
            if (generics) genericTypeKey<T>(qualifier) else typeKey<T>(qualifier)

    /**
     * Create [DependencyKey] for factory.
     */
    inline fun <reified A : Any, reified R : Any> factoryKey(qualifier: Any? = null, generics: Boolean = false): DependencyKey =
            if (generics) genericCompoundTypeKey<A, R>(qualifier) else compoundTypeKey<A, R>(qualifier)

    /**
     * Register a provider by key.
     *
     * @suppress
     */
    fun <T : Any> registerProvider(key: DependencyKey, scope: ProviderScope, override: Boolean, block: ProviderBlock<T>) {
        register(key, UnboundProviderEntry(scope, setupProviderBlock(key, scope, block)), override)
    }

    /**
     * Register a constant by key.
     *
     * @suppress
     */
    fun <T : Any> registerConstant(key: DependencyKey, override: Boolean, value: T) {
        register(key, ConstantEntry(value), override)
    }

    /**
     * Register a factory by key.
     *
     * @suppress
     */
    fun <A : Any, R : Any> registerFactory(key: DependencyKey,
                                           scope: FactoryScope,
                                           override: Boolean = false,
                                           block: FactoryBlock<A, R>) {
        register(key, FactoryEntry(scope, setupFactoryBlock(key, block)), override)
    }

    /**
     * Register a [MembersInjector] provider by key.
     *
     * @suppress
     */
    fun <T : Any> registerMembersInjector(key: DependencyKey, provider: Provider<MembersInjector<T>>) {
        register(key, ProviderEntry(provider), false)
    }

    private fun register(key: DependencyKey, entry: ComponentEntry<*>, override: Boolean) {
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
     * Remove a dependency from the component.
     * Throws an [EntryNotFoundException] if the dependency doesn't exist and [silent] is false (default).
     */
    fun remove(key: DependencyKey, silent: Boolean = false) {
        if (!silent && !registry.containsKey(key)) {
            throw EntryNotFoundException("Can't remove entry with key `$key` because it doesn't exist.")
        }
        registry.remove(key)
        eagerDependencies -= key
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