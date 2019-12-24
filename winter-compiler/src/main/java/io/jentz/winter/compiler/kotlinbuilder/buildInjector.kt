package io.jentz.winter.compiler.kotlinbuilder

import io.jentz.winter.compiler.*

fun buildInjector(
        configuration: ProcessorConfiguration,
        model: InjectorModel
): KotlinFile {
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

        generatedAnnotation(configuration.generatedAnnotationAvailable)

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
                    when (target) {
                        is InjectTargetModel.FieldInjectTarget -> {
                            val fieldName = target.element.simpleName
                            val getInstance = generateGetInstanceCode("graph.", target.element)
                            appendIndent()
                            append("target.$fieldName = ")
                            appendCode(getInstance)
                            newLine()
                        }
                        is InjectTargetModel.SetterInjectTarget -> {
                            val setterName = target.element.simpleName
                            val parameter = target.parameter
                            val getInstance = generateGetInstanceCode("graph.", parameter)
                            appendIndent()
                            append("target.$setterName(")
                            appendCode(getInstance)
                            append(")")
                            newLine()
                        }
                    }
                }
            }
            line()
        }

    }
}
