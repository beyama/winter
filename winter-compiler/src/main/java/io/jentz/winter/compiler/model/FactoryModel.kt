package io.jentz.winter.compiler.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import io.jentz.winter.compiler.*
import io.jentz.winter.inject.Prototype
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Scope
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import io.jentz.winter.Scope as WinterScope

class FactoryModel private constructor(
    val originatingElement: Element,
    val typeElement: TypeElement,
    val constructorElement: ExecutableElement
) {

    companion object {

        fun forInjectAnnotatedConstructor(constructorElement: ExecutableElement) = FactoryModel(
            originatingElement = constructorElement,
            typeElement = constructorElement.enclosingElement as TypeElement,
            constructorElement = constructorElement
        )

        fun forInjectConstructorAnnotatedClass(typeElement: TypeElement): FactoryModel {
            val constructorElements = ElementFilter.constructorsIn(typeElement.enclosedElements)
            val constructorElement = constructorElements.first()

            require(constructorElements.size == 1) {
                "Class `${typeElement.qualifiedName}` is annotated with InjectConstructor " +
                        "and therefore must not have more than one constructor."
            }

            require(constructorElement.getAnnotation(Inject::class.java) == null) {
                "Class `${typeElement.qualifiedName}` is annotated with InjectConstructor " +
                        "and therefore must not have a constructor with Inject annotation."
            }

            return FactoryModel(
                originatingElement = typeElement,
                typeElement = typeElement,
                constructorElement = constructorElement
            )
        }

    }

    val typeName = typeElement.asClassName()

    val scopeAnnotationName: String?

    val scope: WinterScope

    val qualifier: String? = typeElement
        .getAnnotation(Named::class.java)
        ?.value
        ?.takeIf { it.isNotBlank() }

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

        val scopeAnnotations = typeElement.annotationMirrors.map {
            it.annotationType.asElement() as TypeElement
        }.filter {
            it.getAnnotation(Scope::class.java) != null
        }

        if (scopeAnnotations.size > 1) {
            val scopesString = scopeAnnotations.joinToString(", ") { it.qualifiedName.toString() }
            throw IllegalArgumentException(
                "Multiple @Scope qualified annotations found on $typeElement but only one is " +
                        "allowed. ($scopesString})"
            )
        }

        scopeAnnotationName = scopeAnnotations.firstOrNull()?.let { scopeAnnotation ->
            val scopeAnnotationName = scopeAnnotation.qualifiedName.toString()
            val retention = scopeAnnotation.getAnnotation(Retention::class.java)

            if (retention?.value != RetentionPolicy.RUNTIME) {
                throw IllegalArgumentException(
                    "Scope annotation `$scopeAnnotationName` doesn't have RUNTIME retention."
                )
            }

            scopeAnnotationName
        }

        scope = when {
            typeElement.getAnnotation(Prototype::class.java) != null -> WinterScope.Prototype
            scopeAnnotationName == null -> WinterScope.Prototype
            else -> WinterScope.Singleton
        }

    }

}
