package io.jentz.winter.compiler.model

import io.jentz.winter.compiler.hasAccessibleSetter
import io.jentz.winter.compiler.isNullable
import io.jentz.winter.compiler.isPrivate
import io.jentz.winter.compiler.qualifier
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.syntheticMethodForAnnotations
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter

sealed class InjectTargetModel(
    val kmProperty: KmProperty?
) {

    abstract val originatingElement: Element

    abstract val variableElement: VariableElement

    abstract val qualifier: String?

    private val type get() = originatingElement.enclosingElement as TypeElement

    val fqdn get() = "${type.qualifiedName}.${originatingElement.simpleName}"

    val isNullable: Boolean get() = kmProperty?.isNullable ?: variableElement.isNullable

    val propertyQualifier: String? get() = kmProperty
        ?.syntheticMethodForAnnotations
        ?.let { signature ->
            ElementFilter
                .methodsIn(type.enclosedElements)
                .find { it.simpleName.contentEquals(signature.name) }
        }?.qualifier

}

class FieldInjectTarget(
    override val originatingElement: VariableElement,
    kmProperty: KmProperty?
) : InjectTargetModel(kmProperty) {

    override val variableElement: VariableElement = originatingElement

    override val qualifier: String? get() = variableElement.qualifier ?: propertyQualifier

    init {
        if (originatingElement.isPrivate && kmProperty?.hasAccessibleSetter != true) {
            throw IllegalArgumentException("Cannot inject into private fields ($fqdn).")
        }
    }

}

class SetterInjectTarget(
    override val originatingElement: ExecutableElement,
    kmProperty: KmProperty?
) : InjectTargetModel(kmProperty) {

    override val variableElement: VariableElement

    override val qualifier: String?
        get() = variableElement.qualifier
            ?: variableElement.enclosingElement.qualifier
            ?: propertyQualifier

    init {
        if (originatingElement.parameters.size != 1) {
            throw IllegalArgumentException(
                "Setter for setter injection must have exactly one parameter not " +
                        "${originatingElement.parameters.size} ($fqdn)."
            )
        }
        if (originatingElement.isPrivate) {
            throw IllegalArgumentException("Cannot inject into private setter ($fqdn).")
        }

        variableElement = originatingElement.parameters.first()
    }

}
