package io.jentz.winter.compiler.kotlinbuilder

import io.jentz.winter.compiler.*
import javax.lang.model.element.TypeElement

fun buildComponent(
        configuration: ProcessorConfiguration,
        factories: List<ServiceModel>,
        injectors: Map<TypeElement, InjectorModel>
): KotlinFile = buildKotlinFile(
    packageName = configuration.generatedComponentPackage,
    fileName = "generatedComponent",
    originatingElement = null
) {

    import(COMPONENT_CLASS_NAME)
    import(COMPONENT_METHOD_NAME)

    generatedAnnotation(configuration.generatedAnnotationAvailable)
    generatedComponent {
        line()

        val grouped = factories.groupBy { it.scope ?: "_prototype_" }

        grouped.forEach { (scope, factories) ->
            when (scope) {
                "_prototype_" -> {
                    factories.forEach { factory ->
                        prototype(factory, injectors[factory.typeElement])
                    }
                }
                configuration.rootScopeAnnotation -> {
                    factories.forEach { factory ->
                        singleton(factory, injectors[factory.typeElement])
                    }
                }
                else -> {
                    subcomponent(scope) {
                        factories.forEach { factory ->
                            singleton(factory, injectors[factory.typeElement])
                        }
                    }
                }
            }
        }

        line()
    }

}

private typealias ComponentBuilderBlock = ComponentBuilder.() -> Unit

private class ComponentBuilder(
        private val builder: KotlinBuilder
) {

    fun prototype(serviceModel: ServiceModel, injectorModel: InjectorModel?) {
        scope("prototype", serviceModel, injectorModel)
    }

    fun singleton(serviceModel: ServiceModel, injectorModel: InjectorModel?) {
        scope("singleton", serviceModel, injectorModel)
    }

    private fun scope(
            scopeName: String,
            serviceModel: ServiceModel,
            injectorModel: InjectorModel?
    ) {
        val typeName = serviceModel.typeName

        builder.block("$scopeName<${typeName.simpleName}>") {
            import(typeName)

            val params = serviceModel.constructor.parameters
                    .map { generateGetInstanceCode("", it) }
                    .onEach { code -> import(code.imports) }
                    .run {
                        if (serviceModel.constructor.parameters.size > 1) {
                            joinToString(",\n    ", "\n    ", "\n") { it.code }
                        } else {
                            joinToString(", ") { it.code }
                        }
                    }

            val createInstance = "${typeName.simpleName}($params)"

            if (injectorModel == null) {
                line(createInstance)
            } else {
                line("val instance = $createInstance")
                line("${injectorModel.generatedClassName}().invoke(this, instance)")
                line("instance")
            }
        }
    }

    fun subcomponent(scopeName: String, block: ComponentBuilderBlock) {
        builder.block("subcomponent($scopeName::class)") {
            ComponentBuilder(builder).apply(block)
        }
    }

}

private fun KotlinFileBuilder.generatedComponent(block: ComponentBuilderBlock) {
    block("val generatedComponent: Component = component") {
        block(ComponentBuilder(this))
    }
}
