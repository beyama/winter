package io.jentz.winter.compiler.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.*
import io.jentz.winter.compiler.*
import io.jentz.winter.inject.EagerSingleton
import io.jentz.winter.inject.FactoryType
import io.jentz.winter.inject.InjectConstructor
import io.jentz.winter.inject.Prototype
import javax.inject.Inject
import javax.inject.Scope
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter
import io.jentz.winter.Scope as WinterScope

@KotlinPoetMetadataPreview
class FactoryModel private constructor(
    val originatingElement: Element,
    val typeName: ClassName,
    val factoryTypeName: ClassName,
    val scopeAnnotationName: ClassName?,
    val scope: WinterScope,
    val isEagerSingleton: Boolean,
    val qualifier: String?,
    val parameters: List<Parameter>,
    val generatedClassName: ClassName,
    val injectorModel: InjectorModel?
) {

    data class Parameter(val typeName: TypeName, val isNullable: Boolean, val qualifier: String?)

    companion object {

        fun forInjectAnnotatedConstructor(constructorElement: ExecutableElement): FactoryModel {
            val typeElement = constructorElement.enclosingElement as TypeElement

            validateType(typeElement)
            validateConstructor(constructorElement)

            val scopeAnnotationName = getScopeAnnotationName(typeElement)

            val winterScopeAnnotation = getWinterScopeAnnotation(typeElement)

            return FactoryModel(
                originatingElement = constructorElement,
                typeName = typeElement.asClassName(),
                factoryTypeName = getFactoryTypeName(typeElement),
                scopeAnnotationName = scopeAnnotationName,
                scope = getWinterScope(winterScopeAnnotation, scopeAnnotationName),
                isEagerSingleton = winterScopeAnnotation is EagerSingleton,
                qualifier = typeElement.qualifier,
                parameters = buildParameters(constructorElement),
                generatedClassName = getGeneratedClassName(typeElement),
                injectorModel = getInjectorModel(typeElement)
            )
        }

        fun forInjectConstructorAnnotatedClass(typeElement: TypeElement): FactoryModel {
            validateType(typeElement)

            val constructorElements = ElementFilter
                .constructorsIn(typeElement.enclosedElements)
                .filterNot { it.isPrivate || it.isProtected }

            require(constructorElements.size == 1) {
                "Class `${typeElement.qualifiedName}` is annotated with InjectConstructor " +
                        "and therefore must have exactly one non-private constructor."
            }

            val constructorElement = constructorElements.first()

            require(constructorElement.getAnnotation(Inject::class.java) == null) {
                "Class `${typeElement.qualifiedName}` is annotated with InjectConstructor " +
                        "and therefore must not have a constructor with Inject annotation."
            }

            val scopeAnnotationName = getScopeAnnotationName(typeElement)

            val winterScopeAnnotation = getWinterScopeAnnotation(typeElement)

            return FactoryModel(
                originatingElement = typeElement,
                typeName = typeElement.asClassName(),
                factoryTypeName = getFactoryTypeName(typeElement),
                scopeAnnotationName = scopeAnnotationName,
                scope = getWinterScope(winterScopeAnnotation, scopeAnnotationName),
                isEagerSingleton = winterScopeAnnotation is EagerSingleton,
                qualifier = typeElement.qualifier,
                parameters = buildParameters(constructorElement),
                generatedClassName = getGeneratedClassName(typeElement),
                injectorModel = getInjectorModel(typeElement)
            )
        }

        private fun validateType(typeElement: TypeElement) {
            require(!(typeElement.isInnerClass && !typeElement.isStatic)) {
                "Cannot inject a non-static inner class."
            }
            require(!typeElement.isPrivate) {
                "Cannot inject a private class."
            }
            require(!typeElement.isAbstract) {
                "Cannot inject a abstract class."
            }
        }

        private fun validateConstructor(constructorElement: ExecutableElement) {
            require(!constructorElement.isPrivate) {
                "Cannot inject a private constructor."
            }
        }

        private fun getFactoryTypeName(typeElement: TypeElement): ClassName {
            val typeUtils: TypeUtils = DI.graph.instance()

            val typeName: ClassName = typeElement.asClassName()

            val injectAnnotationFactoryType = typeUtils
                .getFactoryTypeFromAnnotation(typeElement, InjectConstructor::class)

            val factoryTypeAnnotationFactoryType = typeUtils
                .getFactoryTypeFromAnnotation(typeElement, FactoryType::class)

            require(!(injectAnnotationFactoryType != null && factoryTypeAnnotationFactoryType != null)) {
                "Factory type can be declared via InjectConstructor or FactoryType annotation but not both."
            }

            return when {
                injectAnnotationFactoryType != null -> injectAnnotationFactoryType.asClassName()
                factoryTypeAnnotationFactoryType != null -> factoryTypeAnnotationFactoryType.asClassName()
                else -> typeName
            }
        }

        private fun getScopeAnnotationName(typeElement: TypeElement): ClassName? {
            val scopeAnnotations = typeElement.annotationMirrors.map {
                it.annotationType.asElement() as TypeElement
            }.filter {
                it.getAnnotation(Scope::class.java) != null
            }

            require(scopeAnnotations.size <= 1) {
                val scopesString = scopeAnnotations.joinToString(", ") { it.qualifiedName.toString() }
                "Multiple scope annotations found but only one is allowed. ($scopesString})"
            }


            return scopeAnnotations.firstOrNull()
                ?.asClassName()
                ?.let { if (it == SINGLETON_ANNOTATION_CLASS_NAME) APPLICATION_SCOPE_CLASS_NAME else it }
        }

        private fun getWinterScope(
            winterScopeAnnotation: Annotation?,
            scopeAnnotationName: ClassName?
        ): WinterScope = when {
            winterScopeAnnotation is Prototype -> WinterScope.Prototype
            winterScopeAnnotation is EagerSingleton -> WinterScope.Singleton
            scopeAnnotationName == null -> WinterScope.Prototype
            else -> WinterScope.Singleton
        }

        private fun getGeneratedClassName(typeElement: TypeElement): ClassName {
            val typeName = typeElement.asClassName()
            return ClassName(
                typeName.packageName,
                "${typeName.simpleNames.joinToString("_")}_WinterFactory"
            )
        }

        private fun getInjectorModel(typeElement: TypeElement): InjectorModel? = typeElement
            .selfAndSuperclasses
            .firstOrNull { it.enclosedElements.any(Element::isInjectFieldOrMethod) }
            ?.let { InjectorModel(it, null, null) }

        private fun getWinterScopeAnnotation(typeElement: TypeElement): Annotation? {
            val eagerSingleton: EagerSingleton? = typeElement.getAnnotation(EagerSingleton::class.java)
            val prototype: Prototype? = typeElement.getAnnotation(Prototype::class.java)

            require(!(eagerSingleton != null && prototype != null)) {
                "Class can either be annotated with EagerSingleton or Prototype but not both."
            }

            return eagerSingleton ?: prototype
        }

        private fun buildParameters(constructorElement: ExecutableElement): List<Parameter> {
            return mergeJavaAndKotlinParameters(constructorElement)
                .map { (javaParam, kotlinParam) ->
                    Parameter(
                        typeName = kotlinParam?.type?.asTypeName()
                            ?: javaParam.asType().asTypeName().kotlinTypeName,
                        isNullable = kotlinParam?.type?.isNullable ?: javaParam.isNullable,
                        qualifier = javaParam.qualifier
                    )
                }
        }

        private fun mergeJavaAndKotlinParameters(
            constructorElement: ExecutableElement
        ): List<Pair<VariableElement, ImmutableKmValueParameter?>> {
            val typeElement = constructorElement.enclosingElement as TypeElement

            val kmConstructors = runCatching {
                typeElement.toImmutableKmClass().constructors
            }.getOrElse { emptyList() }

            val jvmSignature = constructorElement.jvmSignature()
            val kmConstructor = kmConstructors.find { it.signature?.desc == jvmSignature }

            return if (kmConstructor != null) {
                constructorElement.parameters.zip(kmConstructor.valueParameters)
            } else {
                constructorElement.parameters.map { it to null }
            }

        }

    }

}
