package io.jentz.winter.testing

import io.jentz.winter.*
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible

/**
 * Service that gets its instances from a Kotlin property.
 */
internal class PropertyService(
    override val key: TypeKey<Unit, Any>,
    private val source: Any,
    private val property: KProperty1<Any, *>
) : UnboundService<Unit, Any>, BoundService<Unit, Any> {

    override val scope: Scope get() = Scope.Prototype

    override val requiresLifecycleCallbacks: Boolean get() = false

    init {
        property.isAccessible = true
    }

    override fun bind(graph: Graph): BoundService<Unit, Any> = this

    override fun instance(argument: Unit): Any = property.get(source) ?: throw WinterException(
        "Property `${source.javaClass.name}::${property.name} returned null`."
    )

    override fun newInstance(argument: Unit): Any {
        throw IllegalStateException("BUG: Should never been called.")
    }

    override fun postConstruct(argument: Unit, instance: Any) {
    }

    override fun dispose() {
    }

}
