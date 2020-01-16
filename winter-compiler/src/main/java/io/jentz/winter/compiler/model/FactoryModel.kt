package io.jentz.winter.compiler.model

import com.squareup.javapoet.ClassName
import io.jentz.winter.compiler.*
import io.jentz.winter.inject.EagerSingleton
import io.jentz.winter.inject.Prototype
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

    val typeName: ClassName = ClassName.get(typeElement)

    val scopeAnnotationName: ClassName?

    val scope: WinterScope

    val isEagerSingleton: Boolean

    val qualifier: String? = typeElement
        .getAnnotation(Named::class.java)
        ?.value
        ?.takeIf { it.isNotBlank() }

    val generatedClassName: ClassName = ClassName.get(
        typeName.packageName(),
        "${typeName.simpleNames().joinToString("_")}_WinterFactory"
    )

    val injectorModel = typeElement
        .selfAndSuperclasses
        .firstOrNull { it.enclosedElements.any(Element::isInjectFieldOrMethod) }
        ?.let { InjectorModel(it, null, null) }

    init {
        require(!(typeElement.isInnerClass && !typeElement.isStatic)) {
            "Cannot inject a non-static inner class."
        }
        require(!typeElement.isPrivate) {
            "Cannot inject a private class."
        }
        require(!typeElement.isAbstract) {
            "Cannot inject a abstract class."
        }

        val scopeAnnotations = typeElement.annotationMirrors.map {
            it.annotationType.asElement() as TypeElement
        }.filter {
            it.getAnnotation(Scope::class.java) != null
        }

        require(scopeAnnotations.size <= 1) {
            val scopesString = scopeAnnotations.joinToString(", ") { it.qualifiedName.toString() }
            "Multiple scope annotations found but only one is allowed. ($scopesString})"
        }

        scopeAnnotationName = scopeAnnotations.firstOrNull()?.let { ClassName.get(it) }

        isEagerSingleton = typeElement.getAnnotation(EagerSingleton::class.java) != null

        val hasPrototypeAnnotation = typeElement.getAnnotation(Prototype::class.java) != null

        require(!(hasPrototypeAnnotation && isEagerSingleton)) {
            "Class can either be annotated with EagerSingleton or Prototype but not both."
        }

        scope = when {
            hasPrototypeAnnotation -> WinterScope.Prototype
            scopeAnnotationName == null -> WinterScope.Prototype
            else -> WinterScope.Singleton
        }

    }

}
