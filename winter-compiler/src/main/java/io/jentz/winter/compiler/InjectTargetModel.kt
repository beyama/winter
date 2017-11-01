package io.jentz.winter.compiler

import com.squareup.kotlinpoet.CodeBlock
import javax.inject.Named
import javax.lang.model.element.*

sealed class InjectTargetModel {
    abstract val element: Element
    val qualifier: String? get() = element.getAnnotation(Named::class.java)?.value
    val type get() = element.enclosingElement as TypeElement
    val fqdn get() = "${type.qualifiedName}.${element.simpleName}"

    abstract fun codeBlock(): CodeBlock

    class FieldInjectTarget(override val element: VariableElement) : InjectTargetModel() {

        init {
            if (element.modifiers.contains(Modifier.PRIVATE)) {
                throw IllegalArgumentException("Can't inject into private fields ($fqdn).")
            }
        }

        override fun codeBlock(): CodeBlock {
            return CodeBlock.builder().add("target.${element.simpleName} = graph.instance()\n").build()
        }
    }

    class SetterInjectTarget(override val element: ExecutableElement) : InjectTargetModel() {
        init {
            if (element.parameters.size != 1) {
                throw IllegalArgumentException("Setter for setter injection must have exactly one parameter not ${element.parameters.size} ($fqdn).")
            }
            if (element.modifiers.contains(Modifier.PRIVATE)) {
                throw IllegalArgumentException("Can't inject into private setter ($fqdn).")
            }
        }

        override fun codeBlock(): CodeBlock {
            return CodeBlock.builder().add("target.${element.simpleName}(graph.instance())\n").build()
        }
    }
}