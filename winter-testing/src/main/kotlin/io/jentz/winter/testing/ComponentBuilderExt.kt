package io.jentz.winter.testing

import io.jentz.winter.ComponentBuilder
import io.jentz.winter.TypeKey
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

/**
 * Register a service that uses [property][KProperty1] as source for an instance.
 *
 * @param source The instance to retrieve the [property] value from.
 * @param property The [KProperty1] instance.
 * @param override If true this will override an existing service of this type.
 */
fun ComponentBuilder.property(source: Any, property: KProperty1<*, *>, override: Boolean = false) {
    @Suppress("UNCHECKED_CAST")
    register(PropertyService(property.typeKey, source, property as KProperty1<Any, *>), override)
}

/**
 * Register a service that uses the property [propertyName] as source for an instance.
 *
 * @param source The instance to retrieve the property value from.
 * @param propertyName The name of the property.
 * @param override If true this will override an existing service of this type.
 */
fun ComponentBuilder.property(source: Any, propertyName: String, override: Boolean = false) {
    val property = source::class.getDeclaredMemberProperty(propertyName)
    register(PropertyService(property.typeKey, source, property), override)
}

/**
 * Register a service under [key] that uses the property [propertyName] as source for an instance.
 *
 * @param key The key to register the service under.
 * @param source The instance to retrieve the property value from.
 * @param propertyName The name of the property.
 * @param override If true this will override an existing service of this type.
 */
fun ComponentBuilder.property(
    key: TypeKey,
    source: Any,
    propertyName: String,
    override: Boolean = false
) {
    val property = source::class.getDeclaredMemberProperty(propertyName)
    register(PropertyService(key, source, property), override)
}

/**
 * Register all properties that are annotated with Mock or Spy as a provider.
 *
 * @param source The source object to search for Mock and Spy properties.
 */
fun ComponentBuilder.bindAllMocks(source: Any) {
    source::class
        .declaredMemberProperties
        .filter { hasMockAnnotation(it) }
        .forEach { property(source, it, true) }
}

private fun hasMockAnnotation(property: KProperty1<*, *>): Boolean {
    if (property.annotations.any { containsMockOrSpy(it.javaClass.name) }) {
        return true
    }
    val field = property.javaField ?: return false
    return field.annotations.any { containsMockOrSpy(it.annotationClass.java.name) }
}

private fun containsMockOrSpy(string: String): Boolean =
    string.contains("Mock") || string.contains("Spy")
