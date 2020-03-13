package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.asTypeName
import javax.inject.Inject
import javax.inject.Named
import javax.lang.model.element.*
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

val Element.isStatic get() = modifiers.contains(Modifier.STATIC)
val Element.isPrivate get() = modifiers.contains(Modifier.PRIVATE)
val Element.isProtected get() = modifiers.contains(Modifier.PROTECTED)
val Element.isAbstract get() = modifiers.contains(Modifier.ABSTRACT)

val TypeElement.isInnerClass get() = enclosingElement is TypeElement

val VariableElement.isNotNullable: Boolean
    get() = annotationMirrors.any {
        val element = it.annotationType.asElement() as TypeElement
        val qualifiedName = element.qualifiedName.toString()
        notNullAnnotations.contains(qualifiedName)
    }

val VariableElement.isNullable: Boolean get() = !isNotNullable

val Element.qualifier: String? get() = getAnnotation(Named::class.java)?.value?.takeIf { it.isNotBlank() }

val Element.isInjectFieldOrMethod: Boolean
    get() = getAnnotation(Inject::class.java) != null &&
            (kind == ElementKind.FIELD || kind == ElementKind.METHOD)

val TypeElement.selfAndSuperclasses: Sequence<TypeElement>
    get() = generateSequence(this) {
        if (it.superclass.kind == TypeKind.DECLARED) {
            (it.superclass as DeclaredType).asElement() as TypeElement
        } else {
            null
        }
    }

fun ExecutableElement.jvmSignature(): String = buildString {
    append("(")
    parameters.forEach { append(it.asType().jvmSignature()) }
    append(")")
    append(returnType.jvmSignature())
}

private fun TypeMirror.jvmSignature(): String = when (kind) {
    TypeKind.BOOLEAN -> "Z"
    TypeKind.BYTE -> "B"
    TypeKind.SHORT -> "S"
    TypeKind.INT -> "I"
    TypeKind.LONG -> "J"
    TypeKind.CHAR -> "C"
    TypeKind.FLOAT -> "F"
    TypeKind.DOUBLE -> "D"
    TypeKind.VOID -> "V"
    TypeKind.DECLARED -> {
        val className = when (val typeName = asTypeName()) {
            is ParameterizedTypeName -> typeName.rawType
            is ClassName -> typeName
            else -> error("BUG: Unexpected TypeName subtype `${typeName.javaClass.name}` in JVM Signature builder.")
        }
        val jvmName = className.reflectionName().replace(".", "/")
        "L$jvmName;"
    }
    TypeKind.ARRAY -> {
        val arrayType = this as ArrayType
        "[${arrayType.componentType.jvmSignature()}"
    }
    else -> error("BUG: Unexpected TypeKind `$kind` in JVM Signature builder.")
}
