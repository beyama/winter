package io.jentz.winter.compiler.kotlinbuilder

import io.jentz.winter.compiler.COMPONENT_CLASS_NAME
import io.jentz.winter.compiler.COMPONENT_METHOD_NAME
import io.jentz.winter.compiler.ProcessorConfiguration
import io.jentz.winter.compiler.ServiceModel

fun buildComponent(
        configuration: ProcessorConfiguration,
        factories: List<ServiceModel>
): KotlinFile = buildKotlinFile(
    packageName = checkNotNull(configuration.generatedComponentPackage),
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
                        prototype(factory)
                    }
                }
                configuration.rootScopeAnnotation -> {
                    factories.forEach { factory ->
                        singleton(factory)
                    }
                }
                else -> {
                    subcomponent(scope) {
                        factories.forEach { factory ->
                            singleton(factory)
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

    fun prototype(serviceModel: ServiceModel) {
        scope("prototype", serviceModel)
    }

    fun singleton(serviceModel: ServiceModel) {
        scope("singleton", serviceModel)
    }

    private fun scope(scopeName: String, serviceModel: ServiceModel) {
        val typeName = serviceModel.typeName
        builder.import(typeName)
        builder.line("$scopeName(factory = ${serviceModel.generatedClassName}())")
    }

    fun subcomponent(scopeName: String, block: ComponentBuilderBlock) {
        builder.block("subcomponent($scopeName::class)") {
            line()
            ComponentBuilder(builder).apply(block)
            line()
        }
    }

}

private fun KotlinFileBuilder.generatedComponent(block: ComponentBuilderBlock) {
    block("val generatedComponent: Component = component") {
        block(ComponentBuilder(this))
    }
}
