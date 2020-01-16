package io.jentz.winter.compiler.model

import io.jentz.winter.compiler.KotlinMetadata
import io.jentz.winter.compiler.isNullable
import io.jentz.winter.compiler.isPrivate
import io.jentz.winter.compiler.qualifier
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter

sealed class InjectTargetModel(
    val property: KotlinMetadata.Property?
) {

    abstract val originatingElement: Element

    abstract val variableElement: VariableElement

    abstract val qualifier: String?

    private val type get() = originatingElement.enclosingElement as TypeElement

    val fqdn get() = "${type.qualifiedName}.${originatingElement.simpleName}"

    val isNullable: Boolean get() = property?.isNullable ?: variableElement.isNullable

    val propertyQualifier: String? get() = property?.run {
        val methodName = "$name\$annotations"
        val method = ElementFilter
            .methodsIn(type.enclosedElements)
            .find { it.simpleName.contentEquals(methodName) }
        method?.qualifier
    }
}

class FieldInjectTarget(
    override val originatingElement: VariableElement,
    property: KotlinMetadata.Property?
) : InjectTargetModel(property) {

    override val variableElement: VariableElement = originatingElement

    override val qualifier: String? get() = variableElement.qualifier ?: propertyQualifier

    init {
        if (originatingElement.isPrivate && property?.hasAccessibleSetter != true) {
            throw IllegalArgumentException("Can't inject into private fields ($fqdn).")
        }
    }

}

class SetterInjectTarget(
    override val originatingElement: ExecutableElement,
    property: KotlinMetadata.Property?
) : InjectTargetModel(property) {

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
            throw IllegalArgumentException("Can't inject into private setter ($fqdn).")
        }

        variableElement = originatingElement.parameters.first()
    }

}
