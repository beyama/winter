package io.jentz.winter

import io.jentz.winter.plugin.Plugins
import io.jentz.winter.services.BoundService
import io.jentz.winter.services.ConstantService
import io.jentz.winter.services.checkedInstance

private val QUALIFIER_DERIVED = qualifier("_DERIVED_")

private val KEY_GRAPH = erased<Graph>()

/**
 * The object graph class that retrieves and instantiates dependencies registered in its component.
 *
 * An instance is created by calling [Component.createGraph] or [Graph.createSubgraph].
 */
class Graph internal constructor(
    application: WinterApplication,
    parent: Graph?,
    component: Component,
    onCloseCallback: OnCloseCallback?,
    block: ComponentBuilderBlock?
) {

    private sealed class State {

        class Initialized(
            val graph: Graph,
            val component: Component,
            val parent: Graph?,
            val application: WinterApplication,
            val plugins: Plugins,
            val serviceEvaluator: ServiceEvaluator,
            val onCloseCallback: OnCloseCallback?
        ) : State() {
            val registry: MutableMap<TypeKey<*>, BoundService<*>> = hashMapOf()

            var isClosing = false

            init {
                registry[KEY_GRAPH] = ConstantService(KEY_GRAPH, graph)
            }
        }

        data object Closed : State()
    }

    private var state: State

    private inline fun <T> fold(ifClosed: () -> T, ifInitialized: (State.Initialized) -> T): T =
        when (val state = this.state) {
            is State.Closed -> ifClosed()
            is State.Initialized -> ifInitialized(state)
        }

    private inline fun <T> synchronizedFold(
        ifClosed: () -> T,
        ifInitialized: (State.Initialized) -> T
    ): T = synchronized(this) { fold(ifClosed, ifInitialized) }

    private inline fun <T> map(block: (State.Initialized) -> T): T =
        fold({ throw WinterException("Graph is already closed.") }, block)

    private inline fun <T> synchronizedMap(block: (State.Initialized) -> T): T =
        synchronizedFold({ throw WinterException("Graph is already closed.") }, block)

    /**
     * The [WinterApplication] of this graph.
     */
    val application: WinterApplication get() = map { it.application }

    /**
     * The parent [Graph] instance or null if no parent exists.
     */
    val parent: Graph? get() = map { it.parent }

    /**
     * The [Component] instance.
     */
    val component: Component get() = map { it.component }

    /**
     * Indicates if the graph is closed.
     */
    val isClosed: Boolean get() = fold({ true }, { false })

    init {
        val plugins = application.plugins

        val baseComponent = if (plugins.isNotEmpty() || block != null) {
            component.derive {
                block?.invoke(this)
                plugins.forEach { it.graphInitializing(parent, this) }
            }
        } else {
            component
        }

        state = State.Initialized(
            graph = this,
            component = baseComponent,
            parent = parent,
            application = application,
            plugins = plugins,
            serviceEvaluator = ServiceEvaluator(this, plugins),
            onCloseCallback = onCloseCallback
        )

        plugins.forEach { it.graphInitialized(this) }

        instance(eagerDependenciesKey)?.forEach { key ->
            try {
                instance(key)
            } catch (e: EntryNotFoundException) {
                throw DependencyResolutionException(
                    key, "Error resolving eager dependency with key `$key`", e
                )
            }
        }
    }

    /**
     * Retrieve an instance of type `R`.
     *
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return An instance of `R`
     *
     * @throws EntryNotFoundException
     */
    inline fun <reified R : Any?> instance(
        noinline block: ComponentBuilderBlock? = null
    ): R = instance(erased(), block)

    /**
     * Retrieve an instance of type `R`.
     *
     * @param key The [TypeKey] of the service to resolve.
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return An instance of `R`
     *
     * @throws EntryNotFoundException
     */
    @Suppress("UNCHECKED_CAST")
    fun <R : Any?> instance(
        key: TypeKey<R>,
        block: ComponentBuilderBlock? = null
    ): R = checkedService(key)?.checkedInstance(key.isOptional, block) as R

    tailrec fun <R : Any?> service(key: TypeKey<R>, base: Graph? = this): BoundService<R>? =
        base?.synchronizedMap { state ->
            @Suppress("UNCHECKED_CAST")
            return state.registry.getOrPut(key) {
                state.component[key]?.bind(base) ?: return service(key, base.parent)
            } as BoundService<R>
        }

    fun <R: Any?> checkedService(key: TypeKey<R>): BoundService<R>? {
        val service = service(key)
        if (service == null && !key.isOptional)
            throw EntryNotFoundException(key)
        return service
    }

    /**
     * This is called from [BoundService.instance] when a new instance is created.
     * Don't use this method except in custom [BoundService] implementations.
     */
    fun <R : Any?> evaluate(service: BoundService<R>, block: ComponentBuilderBlock?): R =
        synchronizedMap {
            if (block == null) {
                it.serviceEvaluator.evaluate(service, this)
            } else {
                val graph = derive(block)
                try {
                    it.serviceEvaluator.evaluate(service, graph)
                } finally {
                    graph.close()
                }
            }
        }

    private fun derive(block: ComponentBuilderBlock): Graph = map {
        Graph(it.application, this, component(QUALIFIER_DERIVED, block), null, null)
    }

    /**
     * Initialize and return a subgraph by using the subcomponent with [subcomponentQualifier] and
     * this graph as parent.
     *
     * A graph initialized with this method doesn't get closed when its parent gets closed
     * but becomes inconsistent.
     *
     * Use it with caution in cases where you need to initialize a lot of short-lived subgraphs that
     * are managed by you e.g. a per request subgraph on a HTTP server that gets created per
     * request and closed at the end.
     *
     * @param subcomponentQualifier The subcomponentQualifier of the subcomponent.
     * @param block An optional builder block to derive the subcomponent with.
     */
    fun createSubgraph(
        subcomponentQualifier: Qualifier,
        block: ComponentBuilderBlock? = null
    ): Graph = synchronizedMap { state ->
        Graph(
            application = state.application,
            parent = this,
            component = component.subcomponent(subcomponentQualifier),
            onCloseCallback = null,
            block = block
        )
    }

    /**
     * Runs [graph close plugins][io.jentz.winter.plugin.Plugin.graphClose] and marks this graph
     * as closed. All resources get released and every retrieval method will throw an exception
     * if called after closing.
     *
     * Subsequent calls are ignored.
     */
    fun close() {
        synchronizedFold({}) { state ->
            try {
                if (state.isClosing) return

                state.isClosing = true

                state.plugins.forEach { it.graphClose(this) }

                state.registry.values.forEach { it.onClose() }

                state.onCloseCallback?.invoke(this)
            } finally {
                this.state = State.Closed
            }
        }
    }

}
