package io.jentz.winter.compiler.generator

import com.squareup.javapoet.*
import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.compiler.*
import io.jentz.winter.compiler.model.FactoryModel
import io.jentz.winter.inject.Factory
import javax.lang.model.element.Modifier

class FactoryGenerator(
    private val configuration: ProcessorConfiguration,
    private val model: FactoryModel
) {

    fun generate(): JavaFile {
        val typeName = model.typeName
        val factoryTypeName = model.factoryTypeName
        val parameters = model.constructorElement.parameters
        val graphName = Graph::class.java.toClassName()
        val factoryName = Factory::class.java.toClassName()
        val typeKeyName = TypeKey::class.java.toClassName()
        val componentBuilderName = Component.Builder::class.java.toClassName()
        val superInterfaceName = ParameterizedTypeName.get(factoryName, factoryTypeName)

        val interOpMethodName = if (model.isEagerSingleton) "eagerSingleton" else model.scope.name

        val constructorParameters = parameters.map {
            it.asType().toClassName().box().getInstanceCode(it.isNullable, it.qualifier)
        }.toTypedArray()

        val constructorSignature = when (parameters.size) {
            0 -> ""
            1 -> "\$L"
            else -> parameters.joinToString(",\n    ", "\n    ", "\n") { "\$L" }
        }

        val newInstanceCode = CodeBlock
            .of("new \$T($constructorSignature)", typeName, *constructorParameters)

        val newMembersInjectorCode = model.injectorModel
            ?.let { CodeBlock.of("new \$T()", it.generatedClassName) }

        val invokeMethod = MethodSpec
            .methodBuilder("invoke")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(factoryTypeName)
            .addParameter(graphName, "graph", Modifier.FINAL)
            .apply {
                if (newMembersInjectorCode != null) {
                    addStatement("\$T instance = \$L", typeName, newInstanceCode)
                    addStatement("\$L.inject(graph, instance)", newMembersInjectorCode)
                    addStatement("return instance")
                } else {
                    addStatement("return \$L", newInstanceCode)
                }
            }
            .build()

        val registerMethod = MethodSpec
            .methodBuilder("register")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(ParameterizedTypeName.get(typeKeyName, factoryTypeName))
            .addParameter(componentBuilderName, "builder", Modifier.FINAL)
            .addParameter(ClassName.BOOLEAN, "override", Modifier.FINAL)
            .apply {
                model.scopeAnnotationName?.let { annotation ->
                    addStatement("builder.checkComponentQualifier(\$L)", annotation.toGetKotlinKClassCodeBlock())
                }
            }
            .addStatement("TypeKey<\$L> key = \$L", factoryTypeName, factoryTypeName.newTypeKeyCode(model.qualifier))
            .addStatement("\$T.$interOpMethodName(builder, key, override, this)", INTER_OP_CLASS_NAME)
            .addStatement("return key")
            .build()

        val factoryClass = TypeSpec
            .classBuilder(model.generatedClassName)
            .addSuperinterface(superInterfaceName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .generatedAnnotation(configuration.generatedAnnotation)
            .addOriginatingElement(model.originatingElement)
            .addMethod(invokeMethod)
            .addMethod(registerMethod)
            .build()

        return JavaFile
            .builder(model.generatedClassName.packageName(), factoryClass)
            .build()
    }

}