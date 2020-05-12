package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import io.jentz.winter.inject.ApplicationScope
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Singleton

const val OPTION_GENERATED_COMPONENT_PACKAGE = "winterGeneratedComponentPackage"

val GENERATED_ANNOTATION_LEGACY_INTERFACE_NAME =
    ClassName("javax.annotation", "Generated")

val GENERATED_ANNOTATION_JDK9_INTERFACE_NAME =
    ClassName("javax.annotation.processing", "Generated")

val SINGLETON_ANNOTATION_CLASS_NAME = Singleton::class.asClassName()

val APPLICATION_SCOPE_CLASS_NAME = ApplicationScope::class.asClassName()

val ISO8601_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

// see https://kotlinlang.org/docs/reference/java-interop.html#mapped-types
val mappedTypes: Map<TypeName, TypeName> = mapOf(
    ClassName("java.lang", "Object") to ClassName("kotlin", "Any"),
    ClassName("java.lang", "Cloneable") to ClassName("kotlin", "Cloneable"),
    ClassName("java.lang", "Comparable") to ClassName("kotlin", "Comparable"),
    ClassName("java.lang", "Enum") to ClassName("kotlin", "Enum"),
    ClassName("java.lang", "Annotation") to ClassName("kotlin", "Annotation"),
    ClassName("java.lang", "Deprecated") to ClassName("kotlin", "Deprecated"),
    ClassName("java.lang", "CharSequence") to ClassName("kotlin", "CharSequence"),
    ClassName("java.lang", "String") to ClassName("kotlin", "String"),
    ClassName("java.lang", "Number") to ClassName("kotlin", "Number"),
    ClassName("java.lang", "Throwable") to ClassName("kotlin", "Throwable"),
    ClassName("java.lang", "Byte") to ClassName("kotlin", "Byte"),
    ClassName("java.lang", "Short") to ClassName("kotlin", "Short"),
    ClassName("java.lang", "Integer") to ClassName("kotlin", "Int"),
    ClassName("java.lang", "Long") to ClassName("kotlin", "Long"),
    ClassName("java.lang", "Character") to ClassName("kotlin", "Char"),
    ClassName("java.lang", "Float") to ClassName("kotlin", "Float"),
    ClassName("java.lang", "Double") to ClassName("kotlin", "Double"),
    ClassName("java.lang", "Boolean") to ClassName("kotlin", "Boolean"),
    // collections
    ClassName("java.util", "Iterator") to Iterator::class.asClassName(),
    ClassName("java.lang", "Iterable") to Iterable::class.asClassName(),
    ClassName("java.util", "Collection") to Collection::class.asClassName(),
    ClassName("java.util", "Set") to Set::class.asClassName(),
    ClassName("java.util", "List") to List::class.asClassName(),
    ClassName("java.util", "ListIterator") to ListIterator::class.asClassName(),
    ClassName("java.util", "Map") to Map::class.asClassName(),
    ClassName("java.util", "Map", "Entry") to Map.Entry::class.asClassName()
)

val TypeName.kotlinTypeName: TypeName get() {
    if (this is ParameterizedTypeName) {
        val raw = rawType.kotlinTypeName
        if (raw !is ClassName) return this // should never happen
        return raw.parameterizedBy(typeArguments.map { it.kotlinTypeName })
    }

    return mappedTypes.getOrDefault(this, this)
}

val notNullAnnotations = setOf(
    "org.jetbrains.annotations.NotNull",
    "javax.validation.constraints.NotNull",
    "edu.umd.cs.findbugs.annotations.NonNull",
    "javax.annotation.Nonnull",
    "lombok.NonNull",
    "android.support.annotation.NonNull",
    "org.eclipse.jdt.annotation.NonNull"
)
