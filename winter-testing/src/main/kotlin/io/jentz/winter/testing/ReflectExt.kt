package io.jentz.winter.testing

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

internal val KProperty1<*, *>.typeKey: TypeKey<Unit, Any>
    get() {
        val clazz = (returnType.classifier as? KClass<*>)?.javaObjectType
            ?: throw IllegalArgumentException("Can't get return type for property `$name`")
        return ClassTypeKey(clazz, namedAnnotationValue)
    }

internal val KProperty1<*, *>.namedAnnotationValue: String?
    get() = findAnnotationIncludingField<Named>()?.value

internal inline
fun <reified T : Annotation> KProperty1<*, *>.findAnnotationIncludingField(): T? =
    findAnnotation()
        ?: (this as? KMutableProperty1)?.setter?.findAnnotation()
        ?: javaField?.getAnnotation(T::class.java)

internal fun KClass<*>.getDeclaredMemberProperty(name: String): KProperty1<Any, *> {
    val property = this.declaredMemberProperties.find { it.name == name }
        ?: throw WinterException("Property with name `$name` not found.")

    @Suppress("UNCHECKED_CAST")
    return property as KProperty1<Any, *>
}

internal fun KProperty1<*, *>.hasMockAnnotation(): Boolean {
    if (annotations.any { containsMockOrSpy(it.javaClass.name) }) {
        return true
    }
    val field = javaField ?: return false
    return field.annotations.any { containsMockOrSpy(it.annotationClass.java.name) }
}

private fun containsMockOrSpy(string: String): Boolean =
    string.contains("Mock") || string.contains("Spy")
