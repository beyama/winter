package io.jentz.winter.compiler.generator

import io.jentz.winter.compiler.GRAPH_CLASS_NAME
import io.jentz.winter.compiler.Logger
import io.jentz.winter.compiler.MEMBERS_INJECTOR_INTERFACE_NAME
import io.jentz.winter.compiler.ProcessorConfiguration
import io.jentz.winter.compiler.kotlinbuilder.*
import io.jentz.winter.compiler.model.*

class InjectorGenerator(
    private val configuration: ProcessorConfiguration,
    private val model: InjectorModel,
    private val logger: Logger
) {

    fun generate(): KotlinFile {
        val generatedClassName = model.generatedClassName
        val typeName = model.typeName
        val targets = model.targets
        val superclassInjectorClassName = model.superclassInjectorClassName

        return buildKotlinFile(
            packageName = generatedClassName.packageName,
            fileName = generatedClassName.simpleName,
            originatingElement = model.originatingElement
        ) {

            import(GRAPH_CLASS_NAME)
            import(MEMBERS_INJECTOR_INTERFACE_NAME)
            import(typeName)

            generatedAnnotation(configuration.generatedAnnotation)

            val className = generatedClassName.simpleName
            val interfaceName = "${MEMBERS_INJECTOR_INTERFACE_NAME.simpleName}<${typeName.simpleName}>"

            block("class $className : $interfaceName") {

                line()

                val graphClassName = GRAPH_CLASS_NAME.simpleName
                val targetClassName = typeName.simpleName

                block("override fun invoke(graph: $graphClassName, target: $targetClassName)") {

                    if (superclassInjectorClassName != null) {
                        appendIndent()
                        append("$superclassInjectorClassName().invoke(graph, target)")
                        newLine()
                    }

                    targets.forEach { target ->
                        try {
                            generateTarget(target)
                        } catch (t: Throwable) {
                            logger.error(target.originatingElement, t)
                        }
                    }
                }
                line()
            }

        }

    }

    private fun KotlinFileBuilder.generateTarget(target: InjectTargetModel) {
        val getInstance = generateGetInstanceCode("graph.", target.variableElement, target.isNullable, target.qualifier)

        when (target) {
            is FieldInjectTarget -> {
                val fieldName = target.originatingElement.simpleName
                appendIndent()
                append("target.$fieldName = ")
                appendCode(getInstance)
                newLine()
            }
            is SetterInjectTarget -> {
                appendIndent()
                if (target.property != null) {
                    val propertyName = target.property.name
                    append("target.$propertyName = ")
                    appendCode(getInstance)
                } else {
                    val setterName = target.originatingElement.simpleName
                    append("target.$setterName(")
                    appendCode(getInstance)
                    append(")")
                }
                newLine()
            }
        }
    }

}