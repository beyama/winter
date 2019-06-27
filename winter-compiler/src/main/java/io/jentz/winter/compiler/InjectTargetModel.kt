package io.jentz.winter.compiler

import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

sealed class InjectTargetModel {
    abstract val element: Element

    private val type get() = element.enclosingElement as TypeElement

    val fqdn get() = "${type.qualifiedName}.${element.simpleName}"

    class FieldInjectTarget(override val element: VariableElement) : InjectTargetModel() {

        init {
            if (element.isPrivate) {
                throw IllegalArgumentException("Can't inject into private fields ($fqdn).")
            }
        }

    }

    class SetterInjectTarget(override val element: ExecutableElement) : InjectTargetModel() {

        val parameter: VariableElement

        init {
            if (element.parameters.size != 1) {
                throw IllegalArgumentException(
                        "Setter for setter injection must have exactly one parameter not " +
                                "${element.parameters.size} ($fqdn)."
                )
            }
            if (element.isPrivate) {
                throw IllegalArgumentException("Can't inject into private setter ($fqdn).")
            }

            parameter = element.parameters.first()
        }

    }
}
