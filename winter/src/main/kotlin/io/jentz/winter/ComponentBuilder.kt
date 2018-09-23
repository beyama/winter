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
     * Register a prototype scoped factory for instances of type [R].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [R].
     * @param override If true this will override a existing provider of this type.
     * @param factory The factory for type [R].
     */
    @Deprecated(
            message = "Use prototype instead",
            replaceWith = ReplaceWith("prototype(qualifier,generics,override,factory)")
    )
    inline fun <reified R : Any> provider(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline factory: GFactory0<R>
    ) {
        prototype(qualifier, generics, override, null, factory)
    }

    /**
     * Register a prototype scoped factory for an instance of type [R].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [R].
     * @param override If true this will override a existing provider of this type.
     * @param postConstruct A post construct callback.
     * @param factory The factory for type [R].
     */
    inline fun <reified R : Any> prototype(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline postConstruct: GFactoryCallback1<R>? = null,
            noinline factory: GFactory0<R>
    ) {
        val key = typeKey<R>(qualifier, generics)
        val service = UnboundPrototypeService(key, factory, postConstruct)
        register(service, override)
    }

    /**
     * Register a singleton scoped factory for an instance of type [R].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [R].
     * @param override If true this will override a existing provider of this type.
     * @param postConstruct A post construct callback.
     * @param dispose A callback that gets called when the dependency graph gets disposed.
     * @param factory The factory for type [R].
     */
    inline fun <reified R : Any> singleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline postConstruct: GFactoryCallback1<R>? = null,
            noinline dispose: GFactoryCallback1<R>? = null,
            noinline factory: GFactory0<R>
    ) {
        val key = typeKey<R>(qualifier, generics)
        val service = UnboundSingletonService(key, factory, postConstruct, dispose)
        register(service, override)
    }

    /**
     * Register an eager singleton scoped factory for an instance of type [R].
     *
     * This behaves exactly like [singleton] but the instance will be created as soon as the
     * dependency graph is initialize.
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [R].
     * @param override If true this will override a existing provider of this type.
     * @param postConstruct A post construct callback.
     * @param dispose A callback that gets called when the dependency graph gets disposed.
     * @param factory The factory for [R].
     */
    inline fun <reified R : Any> eagerSingleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline postConstruct: GFactoryCallback1<R>? = null,
            noinline dispose: GFactoryCallback1<R>? = null,
            noinline factory: GFactory0<R>
    ) {
        val key = typeKey<R>(qualifier, generics)
        val service = UnboundSingletonService(key, factory, postConstruct, dispose)
        register(service, override)
        eagerDependencies += key
    }

    /**
     * Register a weak singleton scoped factory for an instance of type [R].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [R].
     * @param override If true this will override a existing provider of this type.
     * @param postConstruct A post construct callback.
     * @param factory The factory for type [R].
     */
    inline fun <reified R : Any> weakSingleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline postConstruct: GFactoryCallback1<R>? = null,
            noinline factory: GFactory0<R>
    ) {
        val key = typeKey<R>(qualifier, generics)
        val service = UnboundWeakSingletonService(key, factory, postConstruct)
        register(service, override)
    }

    /**
     * Register a soft singleton scoped factory for an instance of type [R].
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [R].
     * @param override If true this will override a existing provider of this type.
     * @param postConstruct A post construct callback.
     * @param factory The factory for type [R].
     */
    inline fun <reified R : Any> softSingleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline postConstruct: GFactoryCallback1<R>? = null,
            noinline factory: GFactory0<R>
    ) {
        val key = typeKey<R>(qualifier, generics)
        val service = UnboundSoftSingletonService(key, factory, postConstruct)
        register(service, override)
    }

    /**
     * Register a factory from type `(A) -> R`.
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [A] and [R].
     * @param override If true this will override a existing factory of this type.
     * @param postConstruct A post construct callback.
     * @param factory The factory factory.
     */
    inline fun <reified A : Any, reified R : Any> factory(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline postConstruct: GFactoryCallback2<A, R>? = null,
            noinline factory: GFactory1<A, R>
    ) {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        val service = UnboundFactoryService(key, factory, postConstruct)
        register(service, override)
    }

    /**
     * Register a multiton factory from type `(A) -> R`.
     *
     * A multiton factory is only called once per argument and the result is cached like a
     * singleton.
     *
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [A] and [R].
     * @param override If true this will override a existing factory of this type.
     * @param postConstruct A post construct callback.
     * @param dispose A callback that gets called when the dependency graph gets disposed.
     * @param factory The factory factory.
     */
    inline fun <reified A : Any, reified R : Any> multiton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline postConstruct: GFactoryCallback2<A, R>? = null,
            noinline dispose: GFactoryCallback2<A, R>? = null,
            noinline factory: GFactory1<A, R>
    ) {
        val key = compoundTypeKey<A, R>(qualifier, generics)
        val service = UnboundMultitonFactoryService(key, factory, postConstruct, dispose)
        register(service, override)
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
        register(service, override)
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
    internal fun register(service: UnboundService<*, *>, override: Boolean) {
        val key = service.key
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