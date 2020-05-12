package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.jvmInternalName

@KotlinPoetMetadataPreview
val ImmutableKmProperty.hasAccessibleSetter: Boolean
    get() = Flag.IS_PUBLIC(setterFlags) || Flag.IS_INTERNAL(setterFlags)

val KmProperty.isNullable: Boolean get() = Flag.Type.IS_NULLABLE(returnType.flags)

@KotlinPoetMetadataPreview
fun ImmutableKmType.asTypeName(): TypeName {
    val tokens = (classifier as KmClassifier.Class).name.jvmInternalName.split("/")
    val packageParts = tokens.dropLast(1)
    val classParts = tokens.last().split("$")
    val className =  ClassName(packageParts.joinToString("."), *classParts.toTypedArray())
    if (arguments.isNotEmpty()) {
        val args = arguments.mapNotNull { it.type?.asTypeName() }
        return className.parameterizedBy(args)
    }
    return className
}
