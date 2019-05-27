package io.jentz.winter.compiler

import com.squareup.kotlinpoet.asClassName
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

val Element.isStatic get() = modifiers.contains(Modifier.STATIC)
val Element.isPrivate get() = modifiers.contains(Modifier.PRIVATE)
val Element.isAbstract get() = modifiers.contains(Modifier.ABSTRACT)

val TypeElement.isInnerClass get() = enclosingElement is TypeElement

val VariableElement.isProvider: Boolean
    get() {
        val type = asType()
        if (type.kind != TypeKind.DECLARED) return false
        val declared = type as DeclaredType
        return (declared.asElement() as TypeElement).asClassName() == PROVIDER_INTERFACE_NAME
    }

val VariableElement.isLazy: Boolean
    get() {
        val type = asType()
        if (type.kind != TypeKind.DECLARED) return false
        val declared = type as DeclaredType
        return (declared.asElement() as TypeElement).asClassName() == LAZY_INTERFACE_NAME
    }

val VariableElement.isNotNullable: Boolean
    get() = annotationMirrors.any {
        val element = it.annotationType.asElement() as TypeElement
        val qualifiedName = element.qualifiedName.toString()
        notNullAnnotations.contains(qualifiedName)
    }

val VariableElement.isNullable: Boolean get() = !isNotNullable