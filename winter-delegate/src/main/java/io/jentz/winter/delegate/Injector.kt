package io.jentz.winter.delegate

import io.jentz.winter.ComponentBuilderBlock
import io.jentz.winter.Graph
import io.jentz.winter.Provider
import io.jentz.winter.WinterApplication
import io.jentz.winter.WinterApplication.InjectionAdapter
import io.jentz.winter.WinterException
import io.jentz.winter.typeKey
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Injector internal constructor(
    private val target: Any,
    private val application: WinterApplication
): ReadOnlyProperty<Any, Injector> {

    @PublishedApi
    internal val properties = mutableListOf<InjectedProperty<*>>()

    /**
     * Creates a property delegate for an instance of type `R`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> instance(
        qualifier: Any? = null,
        generics: Boolean = false,
        noinline block: ComponentBuilderBlock? = null
    ): InjectedProperty<R> = InstanceProperty<R>(typeKey(qualifier, generics), block)
        .also { properties.add(it) }

    /**
     * Creates a property delegate for a [Provider] of type `() -> R`.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> provider(
        qualifier: Any? = null,
        generics: Boolean = false,
        noinline block: ComponentBuilderBlock? = null
    ): InjectedProperty<Provider<R>> = ProviderProperty(typeKey<R>(qualifier, generics), block)
        .also { properties.add(it) }

    /**
     * Creates a lazy property delegate for an instance of type `R`.
     *
     * The instance gets retrieved/created on first property access.
     *
     * @param qualifier An optional qualifier.
     * @param generics Preserve generic type parameters.
     * @param block An optional builder block to pass runtime dependencies to the factory.
     * @return The created [InjectedProperty].
     */
    inline fun <reified R : Any> lazyInstance(
        qualifier: Any? = null,
        generics: Boolean = false,
        noinline block: ComponentBuilderBlock? = null
    ): InjectedProperty<R> = LazyInstanceProperty(typeKey<R>(qualifier, generics), block)
        .also { properties.add(it) }

    /**
     * Inject dependencies into target by using the dependency graph returned from
     * [InjectionAdapter.get] called with [target].
     *
     * @throws [io.jentz.winter.WinterException] If given [target] type is not supported.
     */
    fun inject() {
        val adapter = application.injectionAdapter ?: throw WinterException(
            "No injection adapter configured."
        )
        val graph = adapter.get(target) ?: throw WinterException(
            "No graph found for instance `$target`."
        )
        inject(graph)
    }

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