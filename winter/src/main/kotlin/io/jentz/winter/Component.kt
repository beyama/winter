package io.jentz.winter

import io.jentz.winter.Component.Builder
import io.jentz.winter.services.AliasService
import io.jentz.winter.services.ConstantService
import io.jentz.winter.services.MapOfProvidersForTypeService
import io.jentz.winter.services.MapOfTypeService
import io.jentz.winter.services.PrototypeService
import io.jentz.winter.services.SetOfProvidersForTypeService
import io.jentz.winter.services.SetOfTypeService
import io.jentz.winter.services.SingletonService
import io.jentz.winter.services.UnboundService
import kotlin.reflect.KClass

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
 *     singleton<MyService> { MyServiceImpl(instance()) }
 * }
 * val derived = appComponent.derive {
 *     prototype<MyOtherService> { MyOtherServiceImpl(instance(), instance()) }
 * }
 * val graph = derived.createGraph { constant<Application>(myApplicationInstance) }
 * ```
 *
 * @see Builder
 */
class Component private constructor(
    /**
     * The components qualifier.
     */
    val qualifier: Qualifier,

    private val registry: Map<TypeKey<*>, UnboundService<*>>,

    private val subcomponentKeys: Set<TypeKey<Component>>
) {

    companion object {
        val EMPTY = Component(Qualifier.App, emptyMap(), emptySet())
    }

    /**
     * Create an extended copy of this component.
     *
     * @param qualifier A qualifier for the new derived component (default: [qualifier]).
     * @param block A builder block that is called in the context of a [Builder].
     * @return A new [Component] that contains all provider of the base component plus the one
     *         defined in the builder block.
     */
    fun derive(
        qualifier: Qualifier = this.qualifier,
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
    fun subcomponent(vararg qualifiers: Qualifier): Component =
        qualifiers.fold(this) { component, qualifier ->
            val key = typeKey<Component>(qualifier)
            val constant = component.registry[key] as? ConstantService<*>
            if (constant == null) {
                val path = qualifiers.joinToString(".") { it.value }
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
        val qualifier: Qualifier,
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

        @PublishedApi
        internal var override = false

        private val registry: MutableMap<TypeKey<*>, UnboundService<*>>
            get() = _registry ?: HashMap(base.registry).also { _registry = it }

        private val subcomponentKeys: MutableSet<TypeKey<Component>>
            get() = _subcomponentKeys
                ?: HashSet(base.subcomponentKeys).also { _subcomponentKeys = it }

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

        /**
         * Service registry functions will override already existing services with the same
         * [TypeKey] inside of the block.
         */
        inline fun override(block: () -> Unit) {
            try {
                override = true
                block()
            } finally {
                override = false
            }
        }

        /**
         * Include dependency from the given component into the new component.
         *
         * @param component The component to include the dependencies from.
         * @param subcomponentIncludeMode Defines the behaviour when a subcomponent with the same
         *                                qualifier already exists.
         */
        @Suppress("UNCHECKED_CAST")
        fun include(
            component: Component,
            subcomponentIncludeMode: SubcomponentIncludeMode = SubcomponentIncludeMode.Merge
        ) {
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
         * @param typeKey The [TypeKey] this service is registered with.
         * @param factory The factory for type [R].
         */
        inline fun <reified R : Any> prototype(
            typeKey: TypeKey<R> = typeKey(),
            noinline factory: GFactory<R>
        ) = register(PrototypeService(typeKey, factory))

        /**
         * Register a singleton scoped factory for an instance of type [R].
         *
         * @param typeKey The [TypeKey] this service is registered with.
         * @param factory The factory for type [R].
         */
        inline fun <reified R : Any> singleton(
            typeKey: TypeKey<R> = typeKey(),
            noinline factory: GFactory<R>
        ) = register(SingletonService(typeKey, factory))

        /**
         * Register a constant of type [R].
         *
         * @param value The value of this constant provider.
         * @param typeKey The [TypeKey] this service is registered with.
         */
        inline fun <reified R : Any> constant(
            value: R,
            typeKey: TypeKey<R> = typeKey()
        ): UnboundService<R> =
            register(ConstantService(typeKey, value))


        /**
         * Register a service that resolves a set of instance of type [R].
         *
         * This may return an empty set if no service of type [R] is registered.
         *
         * @param qualifier An optional qualifier.
         * @param generics If true this will preserve generic information of [R].
         */
        inline fun <reified R : Any> setOfType(
            qualifier: Qualifier? = null,
            generics: Boolean = false,
        ): UnboundService<Set<R>> {
            val key = typeKey<Set<R>>(qualifier, generics = true)
            val typeOfKey = typeKey<R>(generics = generics)
            return register(SetOfTypeService(key, typeOfKey))
        }

        /**
         * Register a service that resolves a set of [providers][Provider] for type [R].
         *
         * This may return an empty set if no service of type [R] is registered.
         *
         * @param qualifier An optional qualifier.
         * @param generics If true this will preserve generic information of [R].
         */
        inline fun <reified R : Any> setOfProvidersForType(
            qualifier: Qualifier? = null,
            generics: Boolean = false
        ): UnboundService<Set<Provider<R>>> {
            val key = typeKey<Set<Provider<R>>>(qualifier, generics = true)
            val typeOfKey = typeKey<R>(generics = generics)
            return register(SetOfProvidersForTypeService(key, typeOfKey))
        }

        /**
         * Register a service that resolves a map of qualifiers to type [R].
         *
         * This may return an empty map if no service of type [R] is registered.
         *
         * @param qualifier An optional qualifier.
         * @param generics If true this will preserve generic information of [R].
         * @param defaultKey The key that is used for a service that was registered without qualifier.
         */
        inline fun <reified R : Any> mapOfType(
            qualifier: Qualifier? = null,
            generics: Boolean = false,
            defaultKey: Qualifier = qualifier("default")
        ): UnboundService<Map<Qualifier, R>> {
            val key = typeKey<Map<Qualifier, R>>(qualifier, generics = true)
            val typeOfKey = typeKey<R>(generics = generics)
            return register(MapOfTypeService(key, typeOfKey, defaultKey))
        }

        /**
         * Register a service that resolves a map of qualifiers to [providers][Provider] for
         * type [R].
         *
         * This may return an empty map if no service of type [R] is registered.
         *
         * @param qualifier An optional qualifier.
         * @param generics If true this will preserve generic information of [R].
         * @param defaultKey The key that is used for a service that was registered without qualifier.
         */
        inline fun <reified R : Any> mapOfProvidersForType(
            qualifier: Qualifier? = null,
            generics: Boolean = false,
            defaultKey: Qualifier = qualifier("default")
        ): UnboundService<Map<Qualifier, Provider<R>>> {
            val key = typeKey<Map<Qualifier, Provider<R>>>(qualifier, generics = true)
            val typeOfKey = typeKey<R>(generics = generics)
            return register(MapOfProvidersForTypeService(key, typeOfKey, defaultKey))
        }

        /**
         * Creates an alias entry.
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
         *
         * @throws WinterException If [newKey] entry already exists and [override] is false.
         */
        fun <R0 : Any?, R1 : Any?> alias(
            targetKey: TypeKey<R0>,
            newKey: TypeKey<R1>,
        ): TypeKey<R0> {
            register(AliasService(targetKey, newKey))
            return targetKey
        }

        /**
         * Create an alias entry for the [UnboundService].
         *
         * Be careful, this method will not check if a type cast is possible.
         *
         * Example:
         * ```
         * singleton {
         *   ReposViewModel(instance())
         * }.alias(typeKey<ViewModel<ReposViewState>>(generics = true))
         * ```
         * @param key The [TypeKey] of the alias.
         */
        inline fun <reified R : Any?> UnboundService<*>.alias(
            key: TypeKey<R> = typeKey()
        ): TypeKey<*> = alias(this.key, key)

        inline fun <reified R : Any, T> UnboundService<T>.alias(
            kClass: KClass<R>,
            qualifier: Qualifier? = null
        ): UnboundService<T> {
            alias(this.key, kClass.typeKey(qualifier))
            return this
        }

        /**
         * Marks a singleton as eager which will instantiate the singleton when the graph opens.
         */
        fun <R: Any> SingletonService<R>.eager(): SingletonService<R> {
            addEagerDependency(key)
            return this
        }

        /**
         * Register a subcomponent.
         *
         * @param qualifier The qualifier of the subcomponent.
         * @param deriveExisting If true an existing subcomponent will be derived and replaced with
         *                       the derived version.
         * @param block A builder block to register provider on the subcomponent.
         */
        fun subcomponent(
            qualifier: Qualifier,
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

            if (doesAlreadyExist && override) {
                remove(key)
            }

            getOrCreateSubcomponentBuilder(key).apply(block)
        }

        /**
         * Check if the registry contains a service with [key]
         *
         * @param key The key to check for.
         * @param checkParent If true this will also check the parent builder for key.
         */
        fun containsKey(key: TypeKey<*>, checkParent: Boolean = true): Boolean {
            val registry = _registry
            val contains = registry?.containsKey(key) ?: base.containsKey(key)
            return if (checkParent) contains || parent?.containsKey(key) == true else contains
        }

        /**
         * Register a [UnboundService].
         *
         * Don't use that except if you add your own [UnboundService] implementations.
         */
        fun <S: UnboundService<*>> register(service: S): S {
            val key = service.key
            val alreadyExists = registry.containsKey(key)

            if (alreadyExists && !override) {
                throw WinterException("Entry with key `$key` already exists.")
            }

            registry[key] = service
            return service
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

        private fun addEagerDependency(key: TypeKey<Any>) {
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
                    try {
                        builder.override = override
                        builder.include(entry.value)
                    } finally {
                        builder.override = false
                    }
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
                    Component(qualifier, base.registry, base.subcomponentKeys)
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
                subcomponentKeys = _subcomponentKeys ?: base.subcomponentKeys
            ).also {
                _registry = null
                _subcomponentKeys = null
                base = it
            }
        }

        private val TypeKey<*>.requireQualifier get() = checkNotNull(qualifier) {
            "BUG! qualifier for subcomponent key must not be null"
        }

    }

}
