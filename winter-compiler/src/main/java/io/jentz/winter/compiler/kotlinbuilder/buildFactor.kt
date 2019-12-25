package io.jentz.winter.compiler.kotlinbuilder

import io.jentz.winter.compiler.FACTORY_INTERFACE_NAME
import io.jentz.winter.compiler.GRAPH_CLASS_NAME
import io.jentz.winter.compiler.ProcessorConfiguration
import io.jentz.winter.compiler.model.FactoryModel

fun buildFactory(
        configuration: ProcessorConfiguration,
        model: FactoryModel
): KotlinFile {
    val generatedClassName = model.generatedClassName
    val typeName = model.typeName

    return buildKotlinFile(
        packageName = generatedClassName.packageName,
        fileName = generatedClassName.simpleName,
        originatingElement = model.originatingElement
    ) {

        import(GRAPH_CLASS_NAME)
        import(FACTORY_INTERFACE_NAME)
        import(typeName)

        generatedAnnotation(configuration.generatedAnnotationAvailable)

        val constructor = model.originatingElement
        val className = generatedClassName.simpleName
        val interfaceName =
            "${FACTORY_INTERFACE_NAME.simpleName}<${GRAPH_CLASS_NAME.simpleName}, ${typeName.simpleName}>"

        val injectorModel = model.injectorModel

        block("class $className : $interfaceName") {

            line()

            val graphClassName = GRAPH_CLASS_NAME.simpleName
            val resultClassName = typeName.simpleName

            block("override fun invoke(graph: $graphClassName): $resultClassName") {

                val params = constructor.parameters
                    .map { generateGetInstanceCode("graph.", it) }
                    .onEach { code -> import(code.imports) }
                    .run {
                        if (constructor.parameters.size > 1) {
                            joinToString(",\n    ", "\n    ", "\n") { it.code }
                        } else {
                            joinToString(", ") { it.code }
                        }
                    }

                val createInstance = "${typeName.simpleName}($params)"

                if (injectorModel == null) {
                    line("return $createInstance")
                } else {
                    line("val instance = $createInstance")
                    line("${injectorModel.generatedClassName}().invoke(graph, instance)")
                    line("return instance")
                }
            }
            line()
        }

    }
}
