package io.jentz.winter.delegate

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.Provider
import io.jentz.winter.TypeKey
import io.jentz.winter.WinterApplication
import io.jentz.winter.erased
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Injector internal constructor(
    private val target: Any,
    val application: WinterApplication
): ReadOnlyProperty<Any, Injector> {

    @PublishedApi
    internal val properties = mutableListOf<InjectedProperty<*>>()

    /**
     * Creates a property delegate for an instance of type `R`.
     *
     * @param key The [TypeKey] of the service to resolve
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The created [InjectedProperty].
     */
    inline operator fun <reified R : Any> invoke(
        key: TypeKey<R> = erased(),
        noinline block: ComponentBuilderBlock? = null
    ) = instance(key, block)

    /**
     * Creates a property delegate for an instance of type `R`.
     *
     * @param key The [TypeKey] of the service to resolve
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> instance(
        key: TypeKey<R> = erased(),
        noinline block: ComponentBuilderBlock? = null
    ): InjectedProperty<R> = InstanceProperty(key, block)
        .also { properties.add(it) }

    /**
     * Creates a property delegate for a [Provider] of type `() -> R`.
     *
     * @param key The [TypeKey] of the service to resolve
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> provider(
        key: TypeKey<R> = erased(),
        noinline block: ComponentBuilderBlock? = null
    ): InjectedProperty<Provider<R>> = ProviderProperty(key, block)
        .also { properties.add(it) }

    /**
     * Creates a lazy property delegate for an instance of type `R`.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param key The [TypeKey] of the service to resolve
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> lazyInstance(
        key: TypeKey<R> = erased(),
        noinline block: ComponentBuilderBlock? = null
    ): InjectedProperty<R> = LazyInstanceProperty(key, block)
        .also { properties.add(it) }

    /**
     * Inject dependencies into properties by using the graph from [Graph.resolveGraph] with
     * the [Injector] [target].
     */
    fun inject() {
        inject(application.graph.resolveGraph(target))
    }

    /**
     * Inject dependencies into properties by using the [graph].
     */
    fun inject(graph: Graph) {
        for (property in properties)
            property.inject(graph)
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): Injector = this
}

operator fun <T: Any> WinterApplication.provideDelegate(
    thisRef: T,
    property: KProperty<*>
): Injector = Injector(thisRef, this)