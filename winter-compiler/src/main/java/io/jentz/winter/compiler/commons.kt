package io.jentz.winter.compiler

import com.squareup.kotlinpoet.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Named
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

const val OPTION_GENERATED_COMPONENT_PACKAGE = "winterGeneratedComponentPackage"
const val OPTION_PRINT_SOURCES = "winterPrintSources" // for printf-debugging during development

val graphClassName = ClassName("io.jentz.winter", "Graph")
val componentClassName = ClassName("io.jentz.winter", "Component")
val factoryInterfaceName = ClassName("io.jentz.winter", "Factory")
val injectorInterfaceName = ClassName("io.jentz.winter.internal", "MembersInjector")
val providerInterfaceName = ClassName("javax.inject", "Provider")
val lazyInterfaceName = ClassName("kotlin", "Lazy")

val generatedAnnotationName = ClassName("javax.annotation", "Generated")

const val generatedFactoryPostfix = "\$\$Factory"
const val generatedInjectorPostfix = "\$\$MembersInjector"

val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").apply { timeZone = TimeZone.getTimeZone("UTC"); }

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

fun isProvider(e: VariableElement): Boolean {
    val type = e.asType()
    if (type.kind != TypeKind.DECLARED) return false
    val declared = type as DeclaredType
    return (declared.asElement() as TypeElement).asClassName() == providerInterfaceName
}

fun isLazy(e: VariableElement): Boolean {
    val type = e.asType()
    if (type.kind != TypeKind.DECLARED) return false
    val declared = type as DeclaredType
    return (declared.asElement() as TypeElement).asClassName() == lazyInterfaceName
}

fun isNotNullable(e: VariableElement): Boolean = e.annotationMirrors.any {
    val element = it.annotationType.asElement() as TypeElement
    val qualifiedName = element.qualifiedName.toString()
    notNullAnnotations.contains(qualifiedName)
}

private fun getInstanceCodeBlock(typeName: TypeName, isNullable: Boolean, qualifier: String?): CodeBlock {
    val hasQualifier = qualifier != null
    return if (hasQualifier && isNullable) CodeBlock.of("graph.instanceOrNull<%T>(%S)", typeName, qualifier)
    else if (hasQualifier && !isNullable) CodeBlock.of("graph.instance<%T>(%S)", typeName, qualifier)
    else if (!hasQualifier && isNullable) CodeBlock.of("graph.instanceOrNull<%T>()", typeName)
    else CodeBlock.of("graph.instance<%T>()", typeName)
}

fun generateGetInstanceCodeBlock(e: VariableElement): CodeBlock {
    val namedAnnotation = e.getAnnotation(Named::class.java) ?: e.enclosingElement.getAnnotation(Named::class.java)
    val qualifier = namedAnnotation?.value
    val isNullable = !isNotNullable(e)

    return when {
        isProvider(e) -> {
            val genericTypeMirror = (e.asType() as DeclaredType).typeArguments.first()
            val typeName = mapTypeName(genericTypeMirror.asTypeName())

            val getter = FunSpec.builder("get")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(if (isNullable) typeName.asNullable() else typeName)
                    .addCode("return %L\n", getInstanceCodeBlock(typeName, isNullable, qualifier))
                    .build()
                    .toString()

            CodeBlock.builder()
                    .beginControlFlow("object : %T<%T>", providerInterfaceName, typeName)
                    .add(getter)
                    .endControlFlow()
                    .build()
        }
        isLazy(e) -> {
            val genericTypeMirror = (e.asType() as DeclaredType).typeArguments.first()
            val typeName = mapTypeName(genericTypeMirror.asTypeName())
            CodeBlock.of("lazy { %L }", getInstanceCodeBlock(typeName, isNullable, qualifier))
        }
        else -> {
            val typeName = mapTypeName(e.asType().asTypeName())
            getInstanceCodeBlock(typeName, isNullable, qualifier)
        }
    }
}

fun generatedAnnotation() = AnnotationSpec.builder(generatedAnnotationName)
        .addMember("value", "%S", WinterProcessor::class.java.name)
        .addMember("date", "%S", iso8601Format.format(Date()))
        .build()

fun generatedComment() = CodeBlock.of("Generated by ${WinterProcessor::class.java.name} at ${iso8601Format.format(Date())}\n")