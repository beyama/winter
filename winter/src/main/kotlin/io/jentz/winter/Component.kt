package io.jentz.winter

import io.jentz.winter.Component.Builder
import io.jentz.winter.inject.ApplicationScope
import io.jentz.winter.inject.Factory
import javax.inject.Singleton

/**
 * The Component stores the dependency providers which are than retrieved and instantiated by an
 * instance of a [graph][Graph].
 *
 * Instances are build by calling [component] with a [builder][Builder] block.
 *
 * Components are immutable but an extended variant can be created by calling [derive] with a
 * builder block.
 *
 * ```
 * val appComponent = component {
 *     provider<MyService>(scope = singleton) { MyServiceImpl(instance()) }
 * }
 * val derived = appComponent.derive {
 *     provider<MyOtherService> { MyOtherServiceImpl(instance(), instance("named")) }
 * }
 * val graph = derived.createGraph { constant<Application>(myAndroidApplication) }
 * ```
 */
class Component private constructor(
    /**
     * The components qualifier.
     */
    val qualifier: Any,

    private val registry: Map<TypeKey<*>, UnboundService<*>>,

    private val subcomponentKeys: Set<TypeKey<Component>>,

    /**
     * Set to true if any of the services requires lifecycle callbacks.
     */
    internal val requiresLifecycleCallbacks: Boolean
) {

    companion object {
        val EMPTY = Component(ApplicationScope::class, emptyMap(), emptySet(), false)
    }

    /**
     * Create an extended copy of this component.
     *
     * @param qualifier A qualifier for the new derived component (default: [qualifier]).
     * @param block A builder block that is called in the context of a [Builder].
     * @return A new [Component] that contains all provider of the base component plus the one
     *         defined in the builder block.
     */
    @JvmOverloads
    fun derive(
        qualifier: Any = this.qualifier,
        block: ComponentBuilderBlock
    ) = Builder(qualifier, this).apply(block).build()

    /**
     * Returns a subcomponent by its qualifier or a nested subcomponent by its path of qualifiers.
     *
     * Main usage for this is to restructure components when using [Builder.include]
     * in conjunction with [Builder.SubcomponentIncludeMode.DoNotInclude].
     *
     * @param qualifiers The qualifier/path of qualifiers of the subcomponent
     * @return The subcomponent
     *
     * @throws EntryNotFoundException If the component does not exist.
     */
    fun subcomponent(vararg qualifiers: Any): Component =
        qualifiers.fold(this) { component, qualifier ->
            val key = typeKey<Component>(qualifier)
            val constant = component.registry[key] as? ConstantService<*>
            if (constant == null) {
                val path = qualifiers.joinToString(".")
                throw EntryNotFoundException(key, "Subcomponent with path [$path] doesn't exist.")
            }
            constant.value as Component
        }

    /**
     * Create a [object graph][Graph] from this component.
     *
     * @param application The [WinterApplication] to use.
     * @param block An optional builder block to extend the component before creating the graph.
     * @return An instance of [Graph] backed by this component.
     */
    @JvmOverloads
    fun createGraph(
        application: WinterApplication = Winter,
        block: ComponentBuilderBlock? = null
    ) = Graph(
        application = application,
        parent = null,
        component = this,
        onCloseCallback = null,
        block = block
    )

    internal fun keys(): Set<TypeKey<*>> = registry.keys

    internal operator fun get(key: TypeKey<*>): UnboundService<*>? = registry[key]

    internal val size: Int get() = registry.size

    internal fun isEmpty(): Boolean = registry.isEmpty()

    internal fun containsKey(typeKey: TypeKey<*>): Boolean = registry.containsKey(typeKey)

    class Builder internal constructor(
        val qualifier: Any,
        private var base: Component = EMPTY,
        private val parent: Builder? = null
    ) {

        enum class SubcomponentIncludeMode {
            /**
             * Do not include subcomponents from the component to include.
             */
            DoNotInclude,
            /**
             * Do not include a subcomponent with a qualifier that is already present in the
             * including component.
             */
            DoNotIncludeIfAlreadyPresent,
            /**
             * Replace an existing subcomponent with same qualifier if already present in the
             * including component.
             */
            Replace,
            /**
             * If a component with the same qualifier already exists in the including component then
             * derive from it and include the subcomponent with same qualifier from the component to
             * include.
             */
            Merge
        }

        init {
            require(qualifier != Singleton::class) {
                "Use `${ApplicationScope::class.java.name}::class` instead of " +
                        "`${Singleton::class.java.name}::class` as component qualifier"
            }
        }

        private val root: Builder = if (parent == null) this else run {
            var base = parent!!
            while (base.parent != null) {
                base = base.parent!!
            }
            base
        }

        private var _registry: MutableMap<TypeKey<*>, UnboundService<*>>? = null

        private var _subcomponentKeys: MutableSet<TypeKey<Component>>? = null

        private var _eagerDependencies: MutableSet<TypeKey<Any>>? = null

        private var _subcomponentBuilders: MutableMap<TypeKey<Component>, Builder>? = null

        private val registry: MutableMap<TypeKey<*>, UnboundService<*>>
            get() = _registry ?: HashMap(base.registry).also { _registry = it }

        private val subcomponentKeys: MutableSet<TypeKey<Component>>
            get() = _subcomponentKeys
                ?: HashSet(base.subcomponentKeys).also { _subcomponentKeys = it }

        private var componentQualifierOverride: Any? = null

        private val eagerDependencies: MutableSet<TypeKey<Any>>
            get() = _eagerDependencies ?: hashSetOf<TypeKey<Any>>().also { set ->
                val base = registry.remove(eagerDependenciesKey)
                @Suppress("UNCHECKED_CAST")
                (base as? ConstantService<Set<TypeKey<Any>>>)?.let { constantService ->
                    set.addAll(constantService.value)
                }
                _eagerDependencies = set
            }

        private val subcomponentBuilders: MutableMap<TypeKey<Component>, Builder>
            get() = _subcomponentBuilders ?: hashMapOf<TypeKey<Component>, Builder>().also {
                _subcomponentBuilders = it
            }

        private var requiresLifecycleCallbacks: Boolean = base.requiresLifecycleCallbacks

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
            checkComponentQualifier(component.qualifier)

            component.registry.forEach { (k, v) ->
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
         * @param onPostConstruct A post construct callback.
         * @param factory The factory for type [R].
         */
        inline fun <reified R : Any> prototype(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline onPostConstruct: GFactoryCallback<R>? = null,
            noinline factory: GFactory<R>
        ): TypeKey<R> {
            val key = typeKey<R>(qualifier, generics)
            val service = UnboundPrototypeService(key, factory, onPostConstruct)
            register(service, override)
            return key
        }

        /**
         * Register a singleton scoped factory for an instance of type [R].
         *
         * @param qualifier An optional qualifier.
         * @param generics If true this will preserve generic information of [R].
         * @param override If true this will override a existing provider of this type.
         * @param onPostConstruct A post construct callback.
         * @param onClose A callback that gets called when the dependency graph gets closed.
         * @param factory The factory for type [R].
         */
        inline fun <reified R : Any> singleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline onPostConstruct: GFactoryCallback<R>? = null,
            noinline onClose: GFactoryCallback<R>? = null,
            noinline factory: GFactory<R>
        ): TypeKey<R> {
            val key = typeKey<R>(qualifier, generics)
            val service = UnboundSingletonService(key, factory, onPostConstruct, onClose)
            register(service, override)
            return key
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
         * @param onPostConstruct A post construct callback.
         * @param onClose A callback that gets called when the dependency graph gets closed.
         * @param factory The factory for [R].
         */
        inline fun <reified R : Any> eagerSingleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline onPostConstruct: GFactoryCallback<R>? = null,
            noinline onClose: GFactoryCallback<R>? = null,
            noinline factory: GFactory<R>
        ): TypeKey<R> {
            val key = typeKey<R>(qualifier, generics)
            val service = UnboundSingletonService(key, factory, onPostConstruct, onClose)
            register(service, override)
            addEagerDependency(key)
            return key
        }

        /**
         * Register a weak singleton scoped factory for an instance of type [R].
         *
         * @param qualifier An optional qualifier.
         * @param generics If true this will preserve generic information of [R].
         * @param override If true this will override a existing provider of this type.
         * @param onPostConstruct A post construct callback.
         * @param factory The factory for type [R].
         */
        inline fun <reified R : Any> weakSingleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline onPostConstruct: GFactoryCallback<R>? = null,
            noinline factory: GFactory<R>
        ): TypeKey<R> {
            val key = typeKey<R>(qualifier, generics)
            val service = UnboundWeakSingletonService(key, factory, onPostConstruct)
            register(service, override)
            return key
        }

        /**
         * Register a soft singleton scoped factory for an instance of type [R].
         *
         * @param qualifier An optional qualifier.
         * @param generics If true this will preserve generic information of [R].
         * @param override If true this will override a existing provider of this type.
         * @param onPostConstruct A post construct callback.
         * @param factory The factory for type [R].
         */
        inline fun <reified R : Any> softSingleton(
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false,
            noinline onPostConstruct: GFactoryCallback<R>? = null,
            noinline factory: GFactory<R>
        ): TypeKey<R> {
            val key = typeKey<R>(qualifier, generics)
            val service = UnboundSoftSingletonService(key, factory, onPostConstruct)
            register(service, override)
            return key
        }

        /**
         * Register a constant of type [R].
         *
         * @param value The value of this constant provider.
         * @param qualifier An optional qualifier.
         * @param generics If true this will preserve generic information of [R].
         * @param override If true this will override an existing factory of this type.
         */
        inline fun <reified R : Any> constant(
            value: R,
            qualifier: Any? = null,
            generics: Boolean = false,
            override: Boolean = false
        ): TypeKey<R> {
            val key = typeKey<R>(qualifier, generics)
            val service = ConstantService(key, value)
            register(service, override)
            return key
        }

        /**
         * Create an alias entry.
         *
         * Be careful, this method will not check if a type cast is possible.
         *
         * Example:
         * ```
         * singleton { ReposViewModel(instance()) }
         *
         * alias(typeKey<ReposViewModel>(), typeKey<ViewModel<ReposViewState>>(generics = true))
         * ```
         *
         * @param targetKey The [TypeKey] of an entry an alias should be created for.
         * @param newKey The alias [TypeKey].
         * @param override If true this will override an existing factory of type [newKey].
         *
         * @throws EntryNotFoundException If [targetKey] entry doesn't exist.
         * @throws WinterException If [newKey] entry already exists and [override] is false.
         */
        fun <R0 : Any, R1 : Any> alias(
            targetKey: TypeKey<R0>,
            newKey: TypeKey<R1>,
            override: Boolean = false
        ): TypeKey<R0> {
            registry[targetKey]
                ?: throw EntryNotFoundException(
                    targetKey,
                    "Entry with key `$targetKey` doesn't exist."
                )
            register(AliasService(targetKey, newKey), override)
            return targetKey
        }

        /**
         * Create an alias entry for a [TypeKey].
         *
         * Be careful, this method will not check if a type cast is possible.
         *
         * Example:
         * ```
         * singleton {
         *   ReposViewModel(instance())
         * }.alias<ViewModel<ReposViewState>>(generics = true)
         * ```
         * @param aliasQualifier The qualifier of the alias entry.
         * @param generics If true this creates a type key that also takes generic type parameters
         *                 into account.
         */
        inline fun <reified R : Any> TypeKey<*>.alias(
            aliasQualifier: Any? = null,
            generics: Boolean = false
        ): TypeKey<*> = alias(this, typeKey<R>(qualifier = aliasQualifier, generics = generics))

        /**
         * Register a generated factory.
         */
        inline fun <reified R : Any> generated(): TypeKey<R> {
            return generatedFactory<R>().register(this)
        }

        /**
         * Loads the generated factory of the given type.
         */
        inline fun <reified R : Any> generatedFactory(): Factory<R> {
            val factoryName = R::class.java.name + "_WinterFactory"
            @Suppress("UNCHECKED_CAST")
            val factory = Class.forName(factoryName) as Class<Factory<R>>
            return factory.getConstructor().newInstance()
        }

        /**
         * Register a subcomponent.
         *
         * @param qualifier The qualifier of the subcomponent.
         * @param override If true an existing subcomponent will be replaced.
         * @param deriveExisting If true an existing subcomponent will be derived and replaced with
         *                       the derived version.
         * @param block A builder block to register provider on the subcomponent.
         */
        fun subcomponent(
            qualifier: Any,
            override: Boolean = false,
            deriveExisting: Boolean = false,
            block: Builder.() -> Unit
        ) {
            if (override && deriveExisting) {
                throw WinterException(
                    "You can either override existing or derive existing but not both."
                )
            }

            val key = typeKey<Component>(qualifier)

            val doesAlreadyExist =
                registry.containsKey(key) || subcomponentBuilders.containsKey(key)

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
                    "Subcomponent with qualifier `$qualifier` does not exist but deriveExisting " +
                            "is true."
                )
            }

            getOrCreateSubcomponentBuilder(key).apply(block)
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
            val wasRemoved = registry.remove(key) != null
                    || _subcomponentBuilders?.remove(key) != null

            if (!silent && !wasRemoved) {
                throw EntryNotFoundException(key, "Entry with key `$key` doesn't exist.")
            }
            subcomponentKeys.remove(key)
            eagerDependencies.remove(key)
        }

        /**
         * Allow a different component qualifier than [qualifier] for [include] and
         * [Factory.register].
         *
         * @param qualifier The qualifier that is allowed in the scope of [block].
         * @param block The block to execute.
         */
        fun allowComponentQualifier(qualifier: Any, block: () -> Unit) {
            val previousOverride = componentQualifierOverride
            componentQualifierOverride = qualifier
            block()
            componentQualifierOverride = previousOverride
        }

        /**
         * Checks if the given [qualifier] meets criteria for [include] and [Factory.register].
         *
         * This is only public because it is needed for annotation preprocessed factories.
         * No need to use that in consumer code.
         */
        fun checkComponentQualifier(qualifier: Any) {
            // Singleton factories can be registered on ApplicationScope by default.
            if (qualifier == Singleton::class
                && (this.qualifier == ApplicationScope::class
                        || componentQualifierOverride == ApplicationScope::class)) return

            if (this.qualifier != qualifier && componentQualifierOverride != qualifier) {
                throw WinterException("Component qualifier `$qualifier` does not match required " +
                        "qualifier `${this.qualifier}`.")
            }
        }

        @PublishedApi
        internal fun addEagerDependency(key: TypeKey<Any>) {
            if (!registry.containsKey(key)) {
                throw WinterException("Key `$key` is not registered.")
            }
            eagerDependencies.add(key)
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
                    if (!registry.containsKey(key)
                        && (_subcomponentBuilders == null
                                || !subcomponentBuilders.containsKey(key))) {
                        addSubcomponentKey(key)
                        registry[key] = entry
                    }
                }
                SubcomponentIncludeMode.Replace -> {
                    _subcomponentBuilders?.remove(key)
                    registry[key] = entry
                }
                SubcomponentIncludeMode.Merge -> {
                    val builder = getOrCreateSubcomponentBuilder(key)
                    builder.include(entry.value)
                }
            }
        }

        private fun getOrCreateSubcomponentBuilder(key: TypeKey<Component>): Builder {
            return subcomponentBuilders.getOrPut(key) {
                val constant = registry.remove(key) as? ConstantService<*>
                val existingSubcomponent = constant?.value as? Component

                if (existingSubcomponent == null) {
                    addSubcomponentKey(key)
                }

                val base = existingSubcomponent ?: EMPTY

                Builder(key.requireQualifier, base, this)
            }
        }

        private fun addSubcomponentKey(key: TypeKey<Component>) {
            if (root.qualifier == key.requireQualifier) {
                throw WinterException(
                    "Subcomponent must have unique qualifier (qualifier `${root.qualifier}` " +
                            "is roots component qualifier)."
                )
            }

            root.checkDescendantsForUniquenessOfKey(key)

            subcomponentKeys.add(key)
        }

        private fun checkDescendantsForUniquenessOfKey(key: TypeKey<Component>) {
            val keys = _subcomponentKeys ?: base.subcomponentKeys
            val registry = _registry ?: base.registry

            for (subcomponentKey in keys) {

                @Suppress("UNCHECKED_CAST")
                val service = registry[subcomponentKey] as? ConstantService<Component>

                if (service != null) {
                    checkDescendantsForUniquenessOfKey(key, service.value)
                } else {
                    val builder = subcomponentBuilders[subcomponentKey] ?: throw WinterException(
                        "BUG: Key `$subcomponentKey` found in subcomponentKeys but component does not exist."
                    )

                    val subcomponentKeys = builder._subcomponentKeys
                        ?: builder.base.subcomponentKeys

                    if (builder.qualifier == key.qualifier || key in subcomponentKeys) {
                        throw WinterException(
                            "Subcomponent with qualifier `${key.qualifier}` already exists."
                        )
                    }

                    builder.checkDescendantsForUniquenessOfKey(key)
                }

            }
        }

        private fun checkDescendantsForUniquenessOfKey(
            key: TypeKey<Component>,
            component: Component
        ) {
            if (component.qualifier == key.qualifier || key in component.subcomponentKeys) {
                throw WinterException(
                    "Subcomponent with qualifier `${key.qualifier}` already exists."
                )
            }

            for (subcomponentKey in component.subcomponentKeys) {
                @Suppress("UNCHECKED_CAST")
                val service = component.registry[key] as? ConstantService<Component>
                    ?: throw WinterException(
                        "BUG: Key `$key` found in subcomponentKeys of " +
                                "component `${component.qualifier}` but component does not exist."
                    )

                checkDescendantsForUniquenessOfKey(key, service.value)
            }
        }

        internal fun build(): Component {
            if (_registry == null) {
                return if (base.qualifier == qualifier) {
                    base
                } else {
                    Component(
                        qualifier,
                        base.registry,
                        base.subcomponentKeys,
                        base.requiresLifecycleCallbacks
                    )
                }
            }

            _subcomponentBuilders?.mapValuesTo(registry) {
                ConstantService(it.key, it.value.build())
            }

            _subcomponentBuilders = null

            _eagerDependencies
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    registry[eagerDependenciesKey] = ConstantService(eagerDependenciesKey, it)
                }

            _eagerDependencies = null

            return Component(
                qualifier = qualifier,
                registry = registry,
                subcomponentKeys = _subcomponentKeys ?: base.subcomponentKeys,
                requiresLifecycleCallbacks = requiresLifecycleCallbacks
            ).also {
                _registry = null
                _subcomponentKeys = null
                base = it
            }
        }

        private val TypeKey<*>.requireQualifier: Any
            get() = checkNotNull(qualifier) {
                "BUG! qualifier for subcomponent key must not be null"
            }

    }

}
