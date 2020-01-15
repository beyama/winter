package io.jentz.winter.compiler

import com.squareup.javapoet.ClassName
import javax.inject.Inject
import javax.inject.Named
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

val Element.isStatic get() = modifiers.contains(Modifier.STATIC)
val Element.isPrivate get() = modifiers.contains(Modifier.PRIVATE)
val Element.isAbstract get() = modifiers.contains(Modifier.ABSTRACT)

val TypeElement.isInnerClass get() = enclosingElement is TypeElement

val VariableElement.isNotNullable: Boolean
    get() = annotationMirrors.any {
        val element = it.annotationType.asElement() as TypeElement
        val qualifiedName = element.qualifiedName.toString()
        notNullAnnotations.contains(qualifiedName)
    }

val VariableElement.isNullable: Boolean get() = !isNotNullable

val Element.qualifier: String? get() = getAnnotation(Named::class.java)?.value

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
