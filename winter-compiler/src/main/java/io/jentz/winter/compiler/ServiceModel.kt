package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import javax.inject.Scope
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class ServiceModel(val originatingElement: ExecutableElement) {

    val typeElement = originatingElement.enclosingElement as TypeElement

    val typeName = typeElement.asClassName()

    val scope: String?

    val generatedClassName = ClassName(
        typeName.packageName,
        "${typeName.simpleNames.joinToString("_")}_WinterFactory"
    )

    val injectorModel = typeElement
        .selfAndSuperclasses
        .firstOrNull { it.enclosedElements.any(Element::isInjectFieldOrMethod) }
        ?.let { InjectorModel(it, null) }

    init {
        if (typeElement.isInnerClass && !typeElement.isStatic) {
            throw IllegalArgumentException("Can't inject a non-static inner class: $typeElement")
        }
        if (typeElement.isPrivate) {
            throw IllegalArgumentException("Can't inject a private class: $typeElement")
        }
        if (typeElement.isAbstract) {
            throw IllegalArgumentException("Can't inject a abstract class: $typeElement")
        }

        val scopes = typeElement.annotationMirrors.map {
            it.annotationType.asElement() as TypeElement
        }.filter {
            it.getAnnotation(Scope::class.java) != null
        }

        if (scopes.size > 1) {
            val scopesString = scopes.joinToString(", ") { it.qualifiedName.toString() }
            throw IllegalArgumentException(
                    "Multiple @Scope qualified annotations found on $typeElement but only one is " +
                            "allowed. ($scopesString})"
            )
        }
        val scopeAnnotation = scopes.firstOrNull()
        scope = if (scopeAnnotation != null) {
            val scopeAnnotationName = scopeAnnotation.qualifiedName.toString()
            val retention = scopeAnnotation.getAnnotation(Retention::class.java)

            if (retention?.value != RetentionPolicy.RUNTIME) {
                throw IllegalArgumentException(
                        "Scope annotation `$scopeAnnotationName` doesn't have RUNTIME retention."
                )
            }

            scopeAnnotationName
        } else {
            null
        }
    }

}
