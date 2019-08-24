package io.jentz.winter.junit4

import io.jentz.winter.ClassTypeKey
import io.jentz.winter.TypeKey
import io.jentz.winter.WinterException
import javax.inject.Named
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField

internal val KProperty1<*, *>.typeKey: TypeKey
    get() {
        val clazz = (returnType.classifier as? KClass<*>)?.javaObjectType
            ?: throw IllegalArgumentException("Can't get return type for property `$name`")
        return ClassTypeKey(clazz, namedAnnotationValue)
    }

internal val KProperty1<*, *>.namedAnnotationValue: String?
    get() = findAnnotationIncludingField<Named>()?.value

internal inline
fun <reified T : Annotation> KProperty1<*, *>.findAnnotationIncludingField(): T? =
    findAnnotation<T>()
        ?: (this as? KMutableProperty1)?.setter?.findAnnotation<T>()
        ?: javaField?.getAnnotation(T::class.java)

/**
 * Get declared member property by name.
 */
fun KClass<*>.getDeclaredMemberProperty(name: String): KProperty1<Any, *> {
    val property = this.declaredMemberProperties.find { it.name == name }
        ?: throw WinterException("Property with name `$name` not found.")

    @Suppress("UNCHECKED_CAST")
    return property as KProperty1<Any, *>
}
