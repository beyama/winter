package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import io.jentz.winter.compiler.kotlinbuilder.KotlinCode
import io.jentz.winter.compiler.kotlinbuilder.buildKotlinCode
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Named
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType

const val OPTION_GENERATED_COMPONENT_PACKAGE = "winterGeneratedComponentPackage"
const val OPTION_ROOT_SCOPE_ANNOTATION = "winterRootScopeAnnotation"
const val OPTION_KAPT_KOTLIN_GENERATED = "kapt.kotlin.generated"

val GRAPH_CLASS_NAME = ClassName("io.jentz.winter", "Graph")
val COMPONENT_CLASS_NAME = ClassName("io.jentz.winter", "Component")
val COMPONENT_METHOD_NAME = ClassName("io.jentz.winter", "component")
val MEMBERS_INJECTOR_INTERFACE_NAME =
        ClassName("io.jentz.winter", "MembersInjector")
val PROVIDER_INTERFACE_NAME = ClassName("javax.inject", "Provider")
val LAZY_INTERFACE_NAME = ClassName("kotlin", "Lazy")
val GENERATED_ANNOTATION_NAME = ClassName("javax.annotation", "Generated")

const val JAVAX_SINGLETON_ANNOTATION_NAME = "javax.inject.Singleton"

val ISO8601_FORMAT =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.getDefault()).apply {
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
        ClassName("java.lang", "Boolean") to ClassName("kotlin", "Boolean")
)

val notNullAnnotations = setOf(
        "org.jetbrains.annotations.NotNull",
        "javax.validation.constraints.NotNull",
        "edu.umd.cs.findbugs.annotations.NonNull",
        "javax.annotation.Nonnull",
        "lombok.NonNull",
        "android.support.annotation.NonNull",
        "org.eclipse.jdt.annotation.NonNull"
)

fun mapTypeName(typeName: TypeName) = mappedTypes.getOrDefault(typeName, typeName)
