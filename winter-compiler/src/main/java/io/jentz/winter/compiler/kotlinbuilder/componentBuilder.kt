package io.jentz.winter.compiler.kotlinbuilder

import io.jentz.winter.compiler.*

typealias ComponentBuilderBlock = ComponentBuilder.() -> Unit

fun buildComponent(
        configuration: ProcessorConfiguration,
        model: ComponentModel
): KotlinFile = buildKotlinFile(configuration.generatedComponentPackage, "generatedComponent") {

    import(COMPONENT_CLASS_NAME)
    import(COMPONENT_METHOD_NAME)

    generatedAnnotation(configuration.generatedAnnotationAvailable)
    generatedComponent {
        line()

        model.injectors.forEach { (_, injector) -> membersInjector(injector) }

        val grouped = model.factories.groupBy { it.scope ?: "_prototype_" }

        grouped.forEach { (scope, factories) ->
            when (scope) {
                "_prototype_" -> {
                    factories.forEach { factory ->
                        prototype(factory, model.injectors[factory.typeElement])
                    }
                }
                "javax.inject.Singleton" -> {
                    factories.forEach { factory ->
                        singleton(factory, model.injectors[factory.typeElement])
                    }
                }
                else -> {
                    subcomponent(scope) {
                        factories.forEach { factory ->
                            singleton(factory, model.injectors[factory.typeElement])
                        }
                    }
                }
            }
        }

        line()
    }

}

class ComponentBuilder(
        private val builder: KotlinBuilder
) {

    fun membersInjector(model: InjectorModel) {
        builder.import(model.typeName)
        builder.import(model.generatedClassName)
        builder.block("membersInjector<${model.typeName.simpleName}>") {
            line("${model.generatedClassName.simpleName}()")
        }
    }

    fun prototype(serviceModel: ServiceModel, injectorModel: InjectorModel?) {
        scope("prototype", serviceModel, injectorModel)
    }

    fun singleton(serviceModel: ServiceModel, injectorModel: InjectorModel?) {
        scope("singleton", serviceModel, injectorModel)
    }

    private fun scope(scopeName: String, serviceModel: ServiceModel, injectorModel: InjectorModel?) {
        val typeName = serviceModel.typeName

        builder.block("$scopeName<${typeName.simpleName}>") {
            import(typeName)

            val params = serviceModel.constructor.parameters
                    .map { generateGetInstanceCode("", it) }
                    .onEach { code -> code.imports.forEach(::import) }
                    .joinToString(", ") { it.code }

            val createInstance = "${typeName.simpleName}($params)"

            if (injectorModel == null) {
                line(createInstance)
            } else {
                line("val instance = $createInstance")
                line("${injectorModel.generatedClassName}().injectMembers(this, instance)")
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

fun KotlinFileBuilder.generatedComponent(block: ComponentBuilderBlock) {
    block("val generatedComponent: Component = component") {
        block(ComponentBuilder(this))
    }
}