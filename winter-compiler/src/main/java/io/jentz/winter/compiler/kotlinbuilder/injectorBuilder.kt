package io.jentz.winter.compiler.kotlinbuilder

import io.jentz.winter.compiler.*

fun buildInjector(
        configuration: ProcessorConfiguration,
        model: InjectorModel
): KotlinFile {
    val generatedClassName = model.generatedClassName
    val typeName = model.typeName
    val targets = model.targets

    return buildKotlinFile(generatedClassName.packageName, generatedClassName.simpleName) {

        import(GRAPH_CLASS_NAME)
        import(MEMBERS_INJECTOR_INTERFACE_NAME)
        import(typeName)

        generatedAnnotation(configuration.generatedAnnotationAvailable)

        block("class ${generatedClassName.simpleName} : ${MEMBERS_INJECTOR_INTERFACE_NAME.simpleName}<${typeName.simpleName}>") {
            line()
            block("override fun injectMembers(graph: ${GRAPH_CLASS_NAME.simpleName}, target: ${typeName.simpleName})") {
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