package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import javax.lang.model.element.TypeElement

class ComponentModel {
    val factories = mutableListOf<FactoryModel>()
    val injectors = mutableMapOf<TypeElement, InjectorModel>()

    fun generate(packageName: String): FileSpec {
        val grouped = factories.groupBy { it.scope ?: "__provider__" }

        val componentBuilder = CodeBlock.builder().beginControlFlow("component")

        injectors.forEach { (_, injector) ->
            val code = CodeBlock.of("membersInjector<%T> { %T() }\n", injector.typeName, escapeGeneratedClassName(injector.generatedClassName))
            componentBuilder.add(code)
        }

        grouped.forEach { (scope, factories) ->
            when (scope) {
                "__provider__" -> factories.forEach { componentBuilder.add(generateProvider(it)) }
                "javax.inject.Singleton" -> factories.forEach { componentBuilder.add(generateSingleton(it)) }
                else -> {
                    componentBuilder
                            .beginControlFlow("subcomponent($scope::class)")
                            .also { subcomponentBlock ->
                                factories.forEach { subcomponentBlock.add(generateSingleton(it)) }
                            }
                            .endControlFlow()
                }
            }
        }

        componentBuilder.endControlFlow()

        return FileSpec.builder(packageName, "generatedComponent")
                .addStaticImport("io.jentz.winter", "component")
                .addProperty(
                        PropertySpec.builder("generatedComponent", componentClassName)
                                .initializer(componentBuilder.build())
                                .build()
                )
                .build()
    }

    fun isEmpty() = factories.isEmpty() && injectors.isEmpty()

    private fun generateProvider(model: FactoryModel) =
            CodeBlock.of("provider<%T> { %T().createInstance(this) }\n", model.typeName, escapeGeneratedClassName(model.generatedClassName))

    private fun generateSingleton(model: FactoryModel) =
            CodeBlock.of("singleton<%T> { %T().createInstance(this) }\n", model.typeName, escapeGeneratedClassName(model.generatedClassName))

    private fun escapeGeneratedClassName(className: ClassName) = ClassName(className.packageName(), "`${className.simpleName()}`")

}