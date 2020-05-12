package io.jentz.winter.compiler.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.compiler.*
import io.jentz.winter.compiler.model.FactoryModel
import io.jentz.winter.inject.Factory

@KotlinPoetMetadataPreview
class FactoryGenerator(
    private val configuration: ProcessorConfiguration,
    private val model: FactoryModel
) {

    fun generate(): FileSpec {
        val typeName = model.typeName
        val factoryTypeName = model.factoryTypeName
        val graphName = Graph::class.asClassName()
        val factoryName = Factory::class.asClassName()
        val typeKeyName = TypeKey::class.asClassName()
        val componentBuilderName = Component.Builder::class.asClassName()
        val superInterfaceName = factoryName.parameterizedBy(factoryTypeName)

        val builderMethodName = if (model.isEagerSingleton) "eagerSingleton" else model.scope.name

        val constructorParameters = model.parameters.map {
            it.typeName.getInstanceCode(it.isNullable, it.qualifier)
        }.toTypedArray()

        val constructorSignature = when (constructorParameters.size) {
            0 -> ""
            1 -> "%L"
            else -> constructorParameters.joinToString(",\n    ", "\n    ", "\n") { "%L" }
        }

        val newInstanceCode = CodeBlock
            .of("%T($constructorSignature)", typeName, *constructorParameters)

        val newMembersInjectorCode = model.injectorModel
            ?.let { CodeBlock.of("%T()", it.generatedClassName) }

        val invokeMethod = FunSpec.builder("invoke")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(factoryTypeName)
            .addParameter("graph", graphName)
            .apply {
                if (newMembersInjectorCode != null) {
                    addStatement("val instance = %L", newInstanceCode)
                    addStatement("%L.inject(graph, instance)", newMembersInjectorCode)
                    addStatement("return instance")
                } else {
                    addStatement("return %L", newInstanceCode)
                }
            }
            .build()

        val qualifierArgument = model.qualifier?.let { "qualifier = \"$it\", " } ?: ""

        val registerMethod = FunSpec.builder("register")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(typeKeyName.parameterizedBy(factoryTypeName))
            .addParameter("builder", componentBuilderName)
            .addParameter("override", BOOLEAN)
            .apply {
                model.scopeAnnotationName?.let { annotation ->
                    addStatement("builder.checkComponentQualifier(%T::class)", annotation)
                }
            }
            .addCode(CodeBlock.of(
                "return builder.$builderMethodName(${qualifierArgument}override = override, factory = this)\n"
            ))
            .build()

        val factoryClass = TypeSpec.classBuilder(model.generatedClassName)
            .addSuperinterface(superInterfaceName)
            .addModifiers(KModifier.PUBLIC)
            .generatedAnnotation(configuration.generatedAnnotation)
            .addOriginatingElement(model.originatingElement)
            .addFunction(invokeMethod)
            .addFunction(registerMethod)
            .build()

        return FileSpec.get(model.generatedClassName.packageName, factoryClass)
    }

}