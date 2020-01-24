package io.jentz.winter.testing

import io.jentz.winter.Component
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

/**
 * Register a service that uses [property][KProperty1] as source for an instance.
 *
 * @param source The instance to retrieve the [property] value from.
 * @param property The [KProperty1] instance.
 * @param override If true this will override an existing service of this type.
 */
internal fun Component.Builder.property(
    source: Any,
    property: KProperty1<Any, *>,
    override: Boolean = false
) {
    register(PropertyService(property.typeKey, source, property), override)
}

/**
 * Register all properties that are annotated with Mock or Spy as a provider.
 *
 * @param source The source object to search for Mock and Spy properties.
 */
fun Component.Builder.bindAllMocks(source: Any) {
    source::class
        .declaredMemberProperties
        .filter { it.hasMockAnnotation() }
        .forEach {
            @Suppress("UNCHECKED_CAST")
            property(source, it as (KProperty1<Any, *>), true)
        }
}
