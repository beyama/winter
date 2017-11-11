package io.jentz.winter.compiler

import com.squareup.kotlinpoet.*
import javax.inject.Named
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

val optionGeneratedComponentPackage = "winterGeneratedComponentPackage"

val graphClassName = ClassName("io.jentz.winter", "Graph")
val componentClassName = ClassName("io.jentz.winter", "Component")
val factoryInterfaceName = ClassName("io.jentz.winter.internal", "Factory")
val injectorInterfaceName = ClassName("io.jentz.winter.internal", "MembersInjector")
val providerInterfaceName = ClassName("javax.inject", "Provider")

val generatedFactoryPostfix = "\$\$Factory"
val generatedInjectorPostfix = "\$\$MembersInjector"

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
        ClassName("java.lang", "Throwable") to ClassName("kotlin", "Throwable")
)

fun mapTypeName(typeName: TypeName) = mappedTypes.getOrDefault(typeName, typeName)

fun isProvider(e: VariableElement): Boolean {
    val type = e.asType()
    if (type.kind != TypeKind.DECLARED) return false
    val declared = type as DeclaredType
    return (declared.asElement() as TypeElement).asClassName() == providerInterfaceName
}

fun generateGetInstanceCodeBlock(e: VariableElement): CodeBlock {
    val annotation = e.getAnnotation(Named::class.java) ?: e.enclosingElement.getAnnotation(Named::class.java)
    val qualifier = annotation?.value

    val getInstanceCodeBlock = if (qualifier != null) {
        CodeBlock.of("graph.instance(%S)", qualifier)
    } else {
        CodeBlock.of("graph.instance()")
    }

    return if (isProvider(e)) {
        val genericTypeMirror = (e.asType() as DeclaredType).typeArguments.first()
        val typeName = mapTypeName(genericTypeMirror.asTypeName())

        val getter = FunSpec.builder("get")
                .addModifiers(KModifier.OVERRIDE)
                .returns(typeName)
                .addCode("return %L\n", getInstanceCodeBlock)
                .build()
                .toString()

        CodeBlock.builder()
                .beginControlFlow("object : %T<%T>", providerInterfaceName, typeName)
                .add(getter)
                .endControlFlow()
                .build()
    } else {
        getInstanceCodeBlock
    }
}