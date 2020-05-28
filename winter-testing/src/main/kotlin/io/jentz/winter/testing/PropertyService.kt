package io.jentz.winter.testing

import io.jentz.winter.*
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible

/**
 * Service that gets its instances from a Kotlin property.
 */
internal class PropertyService(
    override val key: TypeKey<Any>,
    private val source: Any,
    private val property: KProperty1<Any, *>
) : BoundService<Any>(), UnboundService<Any> {

    init {
        property.isAccessible = true
    }

    override val unboundService: UnboundService<Any> get() = this

    override val scope: Scope get() = Scope.Prototype

    override val requiresPostConstructCallback: Boolean get() = false

    override fun bind(graph: Graph): BoundService<Any> = this

    override fun instance(block: ComponentBuilderBlock?): Any = property.get(source)
        ?: throw WinterException(
            "Property `${source.javaClass.name}::${property.name} returned null`."
        )

    override fun newInstance(graph: Graph): Any {
        throw AssertionError("BUG: Should never been called.")
    }

}
