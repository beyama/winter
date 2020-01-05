package io.jentz.winter.compiler.kotlinbuilder

import io.jentz.winter.compiler.COMPONENT_CLASS_NAME
import io.jentz.winter.compiler.COMPONENT_METHOD_NAME
import io.jentz.winter.compiler.ProcessorConfiguration
import io.jentz.winter.compiler.model.FactoryModel

fun buildComponent(
        configuration: ProcessorConfiguration,
        factories: List<FactoryModel>
): KotlinFile = buildKotlinFile(
    packageName = checkNotNull(configuration.generatedComponentPackage),
    fileName = "generatedComponent",
    originatingElement = null
) {

    import(COMPONENT_CLASS_NAME)
    import(COMPONENT_METHOD_NAME)

    generatedAnnotation(configuration.generatedAnnotation)
    generatedComponent {
        line()

        val grouped = factories.groupBy { it.scope ?: "_prototype_" }

        grouped.forEach { (scope, factories) ->
            when (scope) {
                "_prototype_" -> {
                    factories.forEach { factory -> register(factory) }
                }
                configuration.rootScopeAnnotation -> {
                    factories.forEach { factory -> register(factory) }
                }
                else -> {
                    subcomponent(scope) {
                        factories.forEach { factory -> register(factory) }
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

    fun register(model: FactoryModel) {
        builder.line("${model.generatedClassName}().register(this)")
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
