package io.jentz.winter

/**
 * Component builder DSL.
 */
class ComponentBuilder internal constructor(val qualifier: Any?) {
    private val registry: MutableMap<DependencyKey, UnboundService<*, *>> = mutableMapOf()
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
    @PublishedApi
    internal var eagerDependencies: Set<DependencyKey>
        get() = (registry[eagerDependenciesKey] as? ConstantService<Set<DependencyKey>>)
                ?.let { return it.value }
                ?: emptySet()
        set(value) {
            registry[eagerDependenciesKey] = ConstantService(eagerDependenciesKey, value)
        }

    /**
     * Include dependency from the given component into the new component.
     *
     * @param component The component to include the dependency provider from.
     */
    fun include(
            component: Component,
            override: Boolean = true,
            subcomponentIncludeMode: SubcomponentIncludeMode = SubcomponentIncludeMode.Merge
    ) {
        component.dependencies.forEach { (k, v) ->
            when {
                k === eagerDependenciesKey -> {
                    @Suppress("UNCHECKED_CAST")
                    val entry = v as ConstantService<Set<DependencyKey>>
                    eagerDependencies += entry.value
                }
                v is ConstantService<*> && v.value is Component -> {
                    @Suppress("UNCHECKED_CAST")
                    registerSubcomponent(k, v as ConstantService<Component>, subcomponentIncludeMode)
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
     * @param generics If true this will preserve generic information of [T].
     * @param override If true this will override a existing provider of this type.
     * @param block The provider block.
     */
    @Deprecated("Use prototype instead")
    inline fun <reified T : Any> provider(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline block: ProviderBlock<T>
    ) {
        prototype(qualifier, generics, override, block)
    }

    /**
     * Register a provider for instances of type [T].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [T].
     * @param override If true this will override a existing provider of this type.
     * @param block The provider block.
     */
    inline fun <reified T : Any> prototype(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline block: ProviderBlock<T>
    ) {
        val key = typeKey<T>(qualifier, generics)
        val service = UnboundPrototypeService(key, block)
        register(key, service, override)
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
    inline fun <reified T : Any> singleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline postConstruct: (Graph.(T) -> Unit)? = null,
            noinline block: ProviderBlock<T>
    ) {
        val key = typeKey<T>(qualifier, generics)
        val service = UnboundSingletonService(key, block, postConstruct)
        register(key, service, override)
    }

    /**
     * Register an eager singleton scoped provider for an instance of type [T].
     *
     * The instance will be created as soon as the component is initialized.
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [T].
     * @param override If true this will override a existing provider of this type.
     * @param block The provider block.
     */
    inline fun <reified T : Any> eagerSingleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline block: ProviderBlock<T>
    ) {
        val key = typeKey<T>(qualifier, generics)
        val service = UnboundSingletonService(key, block)
        register(key, service, override)
        eagerDependencies += key
    }

    /**
     * Register a factory that takes [A] and returns [R].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [A] and [R].
     * @param override If true this will override a existing factory of this type.
     * @param block The factory block.
     */
    inline fun <reified A : Any, reified R : Any> factory(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline block: FactoryBlock<A, R>
    ) {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        val service = UnboundFactoryService(key, block)
        register(key, service, override)
    }

    /**
     * Register a multiton factory that takes [A] and returns [R].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [A] and [R].
     * @param override If true this will override a existing factory of this type.
     * @param block The factory block.
     */
    inline fun <reified A : Any, reified R : Any> multiton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline block: FactoryBlock<A, R>
    ) {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        val service = UnboundMultitonFactoryService(key, block)
        register(key, service, override)
    }

    /**
     * Register a constant of type [T].
     *
     * @param value The value of this constant provider.
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [T].
     * @param override If true this will override a existing provider of this type.
     */
    inline fun <reified T : Any> constant(
            value: T,
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false
    ) {
        val key = typeKey<T>(qualifier, generics)
        val service = ConstantService(key, value)
        register(key, service, override)
    }

    /**
     * Register a members injector for [T].
     */
//    inline fun <reified T : Any> membersInjector(noinline provider: Provider<MembersInjector<T>>) {
//        registerProvider(membersInjectorKey<T>(), false, provider)
//    }

    /**
     * Register a subcomponent.
     *
     * @param qualifier The qualifier of the subcomponent.
     * @param override If true an existing subcomponent will be replaced.
     * @param deriveExisting If true an existing subcomponent will be derived and replaced with the derived version.
     * @param block A builder block to register provider on the subcomponent.
     */
    fun subcomponent(
            qualifier: Any,
            override: Boolean = false,
            deriveExisting: Boolean = false,
            block: ComponentBuilder.() -> Unit
    ) {
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

    @PublishedApi
    internal fun register(
            key: DependencyKey,
            service: UnboundService<*, *>,
            override: Boolean
    ) {
        val alreadyExists = registry.containsKey(key)

        if (alreadyExists && !override) {
            throw WinterException("Entry with key `$key` already exists.")
        }

        if (!alreadyExists && override) {
            throw WinterException("Entry with key `$key` doesn't exist but override is true.")
        }

        registry[key] = service
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
                                     entry: ConstantService<Component>,
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

        val builders = subcomponentBuilders
                ?: mutableMapOf<DependencyKey, ComponentBuilder>().also { subcomponentBuilders = it }
        val constant = registry.remove(key) as? ConstantService<*>
        val existingSubcomponent = constant?.value as? Component

        return ComponentBuilder(key.qualifier).also { builder ->
            existingSubcomponent?.let { builder.include(it) }
            builders[key] = builder
        }
    }

    internal fun build(): Component {
        subcomponentBuilders?.mapValuesTo(registry) { ConstantService(it.key, it.value.build()) }
        return Component(qualifier, registry.toMap())
    }
}