package io.jentz.winter.compiler.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import io.jentz.winter.Component
import io.jentz.winter.compiler.APPLICATION_SCOPE_CLASS_NAME
import io.jentz.winter.compiler.ProcessorConfiguration
import io.jentz.winter.compiler.model.FactoryModel

@KotlinPoetMetadataPreview
class ComponentGenerator(
    private val configuration: ProcessorConfiguration,
    private val factories: List<FactoryModel>
) {

    private val packageName = checkNotNull(configuration.generatedComponentPackage) {
        "BUG: ComponentGenerator instantiated but package is null."
    }

    private val generatedClassName = ClassName(packageName, "GeneratedComponent")

    fun generate(): FileSpec {
        val groupedFactories = factories.groupBy {
            it.scopeAnnotationName ?: APPLICATION_SCOPE_CLASS_NAME
        }

        val generatedComponent = CodeBlock.builder()
            .beginControlFlow("component(%S)", "generated")
            .apply {
                groupedFactories.forEach { (scopeName, factories) ->
                    beginControlFlow("subcomponent(%T::class)", scopeName)
                    factories.forEach { factory ->
                        addStatement("%T().register(this, false)", factory.generatedClassName)
                    }
                    endControlFlow()
                }
            }
            .endControlFlow()
            .build()

        return FileSpec.builder(generatedClassName.packageName, "generatedComponent")
            .addImport("io.jentz.winter", "component")
            .addProperty(
                PropertySpec.builder("generatedComponent", Component::class.asClassName())
                    .generatedAnnotation(configuration.generatedAnnotation)
                    .initializer(generatedComponent)
                    .build()
            )
            .build()
    }

}
