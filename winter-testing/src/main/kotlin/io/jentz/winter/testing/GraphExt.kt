package io.jentz.winter.testing

import io.jentz.winter.Graph
import io.jentz.winter.WinterException
import javax.inject.Inject
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Injects dependencies into all properties annotated with [Inject] by using reflection.
 */
fun Graph.injectWithReflection(target: Any) {
    target::class
        .declaredMemberProperties
        .filter { it.findAnnotationIncludingField<Inject>() != null }
        .forEach { property ->
            property.isAccessible = true

            val instance = instanceOrNullByKey<Any, Any>(property.typeKey, Unit)
            val field = property.javaField

            when {
                property is KMutableProperty1 -> {
                    @Suppress("UNCHECKED_CAST")
                    (property as KMutableProperty1<Any, Any?>).set(target, instance)
                }
                field != null -> {
                    field.set(target, instance)
                }
                else -> {
                    throw WinterException(
                        "Can't set property `${target.javaClass.name}::${property.name}` " +
                                "no setter and no backing field found."
                    )
                }
            }

        }
}
