package io.jentz.winter

/**
 * Component builder DSL.
 */
class ComponentBuilder internal constructor(val qualifier: Any) {

    enum class SubcomponentIncludeMode {
        /**
         * Do not include subcomponents from the component to include.
         */
        DoNotInclude,
        /**
         * Do not include a subcomponent with a qualifier that is already present in the including
         * component.
         */
        DoNotIncludeIfAlreadyPresent,
        /**
         * Replace an existing subcomponent with same qualifier if already present in the including
         * component.
         */
        Replace,
        /**
         * If a component with the same qualifier already exists in the including component then
         * derive from it and include the subcomponent with same qualifier from the component to
         * include.
         */
        Merge
    }

    private val registry: MutableMap<TypeKey<*>, UnboundService<*>> = mutableMapOf()
    private var requiresLifecycleCallbacks: Boolean = false
    private var eagerDependencies: MutableSet<TypeKey<Any>> = mutableSetOf()
    private var subcomponentBuilders: MutableMap<TypeKey<Component>, ComponentBuilder>? = null

    /**
     * Include dependency from the given component into the new component.
     *
     * @param component The component to include the dependencies from.
     * @param override Set to false to throw an exception if a dependency already exists
     *                 otherwise it will be replaced.
     * @param subcomponentIncludeMode Defines the behaviour when a subcomponent with the same
     *                                qualifier already exists.
     */
    @Suppress("UNCHECKED_CAST")
    fun include(
        component: Component,
        override: Boolean = true,
        subcomponentIncludeMode: SubcomponentIncludeMode = SubcomponentIncludeMode.Merge
    ) {
        component.forEach { (k, v) ->
            when {
                k === eagerDependenciesKey -> {
                    val entry = v as ConstantService<Set<TypeKey<Any>>>
                    eagerDependencies.addAll(entry.value)
                }
                v is ConstantService<*> && v.value is Component -> {
                    val key = k as TypeKey<Component>
                    val service = v as ConstantService<Component>
                    registerSubcomponent(key, service, subcomponentIncludeMode)
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
        noinline postConstruct: GFactoryCallback<R>? = null,
        noinline factory: GFactory<R>
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
        noinline postConstruct: GFactoryCallback<R>? = null,
        noinline dispose: GFactoryCallback<R>? = null,
        noinline factory: GFactory<R>
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
        noinline postConstruct: GFactoryCallback<R>? = null,
        noinline dispose: GFactoryCallback<R>? = null,
        noinline factory: GFactory<R>
    ) {
        val key = typeKey<R>(qualifier, generics)
        val service = UnboundSingletonService(key, factory, postConstruct, dispose)
        register(service, override)
        addEagerDependency(key)
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
        noinline postConstruct: GFactoryCallback<R>? = null,
        noinline factory: GFactory<R>
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
        noinline postConstruct: GFactoryCallback<R>? = null,
        noinline factory: GFactory<R>
    ) {
        val key = typeKey<R>(qualifier, generics)
        val service = UnboundSoftSingletonService(key, factory, postConstruct)
        register(service, override)
    }

    /**
     * Register a constant of type [T].
     *
     * @param value The value of this constant provider.
     * @param qualifier An optional qualifier.
     * @param generics If true this will preserve generic information of [T].
     * @param override If true this will override an existing factory of this type.
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
     * Loads the generated factory of the given type.
     *
     * Useful in conjunction with a scope method like:
     *
     * ```
     * singleton<Service>(factory = generated)
     * ```
     */
    inline fun <reified R : Any> generated(): GFactory<R> {
        val factoryName = R::class.java.name + "_WinterFactory"
        @Suppress("UNCHECKED_CAST")
        val factory = Class.forName(factoryName) as Class<Factory<Graph, R>>
        return factory.getConstructor().newInstance()
    }

    /**
     * Register a subcomponent.
     *
     * @param qualifier The qualifier of the subcomponent.
     * @param override If true an existing subcomponent will be replaced.
     * @param deriveExisting If true an existing subcomponent will be derived and replaced with the
     *                       derived version.
     * @param block A builder block to register provider on the subcomponent.
     */
    fun subcomponent(
        qualifier: Any,
        override: Boolean = false,
        deriveExisting: Boolean = false,
        block: ComponentBuilder.() -> Unit
    ) {
        if (override && deriveExisting) {
            throw WinterException(
                "You can either override existing or derive existing but not both."
            )
        }

        val key = typeKey<Component>(qualifier)

        val doesAlreadyExist =
            registry.containsKey(key) || subcomponentBuilders?.containsKey(key) == true

        if (doesAlreadyExist && !(override || deriveExisting)) {
            throw WinterException("Subcomponent with qualifier `$qualifier` already exists.")
        }

        if (!doesAlreadyExist && override) {
            throw WinterException(
                "Subcomponent with qualifier `$qualifier` doesn't exist but override is true."
            )
        }

        if (!doesAlreadyExist && deriveExisting) {
            throw WinterException(
                "Subcomponent with qualifier `$qualifier` doesn't exist but deriveExisting is true."
            )
        }

        getOrCreateSubcomponentBuilder(key).also(block)
    }

    /**
     * Register a [UnboundService].
     *
     * Don't use that except if you add your own [UnboundService] implementations.
     */
    fun register(service: UnboundService<*>, override: Boolean) {
        val key = service.key
        val alreadyExists = registry.containsKey(key)

        if (alreadyExists && !override) {
            throw WinterException("Entry with key `$key` already exists.")
        }

        if (!requiresLifecycleCallbacks) {
            requiresLifecycleCallbacks = service.requiresLifecycleCallbacks
        }

        registry[key] = service
    }

    /**
     * Remove a dependency from the component.
     * Throws an [EntryNotFoundException] if the dependency doesn't exist and [silent] is false.
     */
    fun remove(key: TypeKey<*>, silent: Boolean = false) {
        if (!silent && !registry.containsKey(key)) {
            throw EntryNotFoundException(key, "Entry with key `$key` doesn't exist.")
        }
        registry.remove(key)
        removeEagerDependency(key)
    }

    /**
     * Create an alias entry.
     *
     * Be careful this method will not check if a type cast is possible.
     *
     * @param targetKey The [TypeKey] of an entry an alias should be created for.
     * @param newKey The alias [TypeKey].
     * @param override If true this will override an existing factory of type [newKey].
     *
     * @throws EntryNotFoundException If [targetKey] entry doesn't exist.
     * @throws WinterException If [newKey] entry already exists and [override] is false.
     */
    fun <R0: Any, R1: Any> alias(
        targetKey: TypeKey<R0>,
        newKey: TypeKey<R1>,
        override: Boolean = false
    ) {
        registry[targetKey]
            ?: throw EntryNotFoundException(targetKey, "Entry with key `$targetKey` doesn't exist.")
        register(AliasService(targetKey, newKey), override)
    }

    @PublishedApi
    internal fun addEagerDependency(key: TypeKey<Any>) {
        if (!registry.containsKey(key)) {
            throw WinterException("Key `$key` is not registered.")
        }
        eagerDependencies.add(key)
    }

    private fun removeEagerDependency(key: TypeKey<*>) {
        eagerDependencies.remove(key)
    }

    private fun registerSubcomponent(
        key: TypeKey<Component>,
        entry: ConstantService<Component>,
        subcomponentIncludeMode: SubcomponentIncludeMode
    ) {
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

    private fun getOrCreateSubcomponentBuilder(key: TypeKey<Component>): ComponentBuilder {
        val qualifier = requireNotNull(key.qualifier) {
            "BUG! qualifier for sub-component key must not be null"
        }

        val builders = subcomponentBuilders ?: mutableMapOf()
        subcomponentBuilders = builders

        return builders.getOrPut(key) {
            ComponentBuilder(qualifier).also { builder ->
                val constant = registry.remove(key) as? ConstantService<*>
                val existingSubcomponent = constant?.value as? Component
                existingSubcomponent?.let { builder.include(it) }
            }
        }
    }

    internal fun build(): Component {
        subcomponentBuilders?.mapValuesTo(registry) { ConstantService(it.key, it.value.build()) }
        subcomponentBuilders = null

        eagerDependencies
            .takeIf { it.isNotEmpty() }
            ?.let { register(ConstantService(eagerDependenciesKey, it.toSet()), false) }

        return Component(qualifier, registry.toMap(), requiresLifecycleCallbacks)
    }
}
