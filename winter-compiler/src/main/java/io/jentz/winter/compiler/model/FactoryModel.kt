package io.jentz.winter.compiler.model

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import io.jentz.winter.compiler.*
import io.jentz.winter.delegate.inject
import io.jentz.winter.inject.EagerSingleton
import io.jentz.winter.inject.FactoryType
import io.jentz.winter.inject.InjectConstructor
import io.jentz.winter.inject.Prototype
import javax.inject.Inject
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
            val constructorElements = ElementFilter
                .constructorsIn(typeElement.enclosedElements)
                .filterNot { it.isPrivate }

            require(constructorElements.size == 1) {
                "Class `${typeElement.qualifiedName}` is annotated with InjectConstructor " +
                        "and therefore must have exactly one non-private constructor."
            }

            val constructorElement = constructorElements.first()

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

    private val typeUtils: TypeUtils by inject()

    val typeName: ClassName = ClassName.get(typeElement)

    val factoryTypeName: TypeName

    val scopeAnnotationName: ClassName?

    val scope: WinterScope

    val isEagerSingleton: Boolean

    val qualifier: String? = typeElement.qualifier

    val generatedClassName: ClassName = ClassName.get(
        typeName.packageName(),
        "${typeName.simpleNames().joinToString("_")}_WinterFactory"
    )

    val injectorModel = typeElement
        .selfAndSuperclasses
        .firstOrNull { it.enclosedElements.any(Element::isInjectFieldOrMethod) }
        ?.let { InjectorModel(it, null, null) }

    init {
        DI.inject(this)

        require(!(typeElement.isInnerClass && !typeElement.isStatic)) {
            "Cannot inject a non-static inner class."
        }
        require(!typeElement.isPrivate) {
            "Cannot inject a private class."
        }
        require(!typeElement.isAbstract) {
            "Cannot inject a abstract class."
        }

        require(!constructorElement.isPrivate) {
            "Cannot inject a private constructor."
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

        val injectAnnotationFactoryType = typeUtils
            .getFactoryTypeFromAnnotation(typeElement, InjectConstructor::class)

        val factoryTypeAnnotationFactoryType = typeUtils
            .getFactoryTypeFromAnnotation(typeElement, FactoryType::class)

        require(!(injectAnnotationFactoryType != null && factoryTypeAnnotationFactoryType != null)) {
            "Factory type can be declared via InjectConstructor or FactoryType annotation but not both."
        }

        factoryTypeName = when {
            injectAnnotationFactoryType != null -> ClassName.get(injectAnnotationFactoryType)
            factoryTypeAnnotationFactoryType != null -> ClassName.get(factoryTypeAnnotationFactoryType)
            else -> typeName
        }

        scopeAnnotationName = scopeAnnotations.firstOrNull()
            ?.toClassName()
            ?.let { if (it == SINGLETON_ANNOTAION_CLASS_NAME) APPLICATION_SCOPE_CLASS_NAME else it }

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
