package io.jentz.winter.compiler

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KClass

class TypeUtils(
    private val elements: Elements,
    private val types: Types
) {

    fun getFactoryTypeFromAnnotation(
        typeElement: TypeElement,
        annotation: KClass<out Annotation>
    ): TypeElement? = getAnnotationMirror(typeElement, annotation)
        ?.let { getAnnotationValue(it, "value") }
        ?.let { it.value as? TypeMirror }
        ?.let { types.asElement(it) as TypeElement }
        ?.takeUnless { it.qualifiedName.contentEquals(Nothing::class.java.name) }
        ?.also { validateFactoryType(typeElement, it) }

    private fun getAnnotationMirror(element: Element, annotation: KClass<out Annotation>): AnnotationMirror? {
        val annotationName = annotation.java.name
        return element.annotationMirrors.find { it.annotationType.toString() == annotationName }
    }

    private fun getAnnotationValue(mirror: AnnotationMirror, name: String): AnnotationValue? = elements
        .getElementValuesWithDefaults(mirror)
        .entries
        .find { entry -> entry.key.simpleName.toString() == name }
        ?.value

    private fun validateFactoryType(typeElement: TypeElement, factoryTypeElement: TypeElement) {
        require(factoryTypeElement.typeParameters.isEmpty()) {
            "The factory type must not be a generic type (${factoryTypeElement.qualifiedName})."
        }
        require(isAssignable(typeElement, factoryTypeElement)) {
            "Type $typeElement is not assignable to $factoryTypeElement."
        }
    }

    private fun isAssignable(typeElement: TypeElement, typeElement1: TypeElement): Boolean =
        types.isAssignable(typeElement.asType(), typeElement1.asType())

}
