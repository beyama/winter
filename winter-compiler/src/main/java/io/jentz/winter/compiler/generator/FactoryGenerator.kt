package io.jentz.winter.compiler.generator

import com.squareup.javapoet.*
import io.jentz.winter.Component
import io.jentz.winter.Graph
import io.jentz.winter.TypeKey
import io.jentz.winter.compiler.ProcessorConfiguration
import io.jentz.winter.compiler.isNullable
import io.jentz.winter.compiler.model.FactoryModel
import io.jentz.winter.compiler.qualifier
import io.jentz.winter.inject.Factory
import io.jentz.winter.inject.InterOp
import javax.lang.model.element.Modifier

class FactoryGenerator(
    private val configuration: ProcessorConfiguration,
    private val model: FactoryModel
) {

    fun generate(): JavaFile {
        val graphName = ClassName.get(Graph::class.java)
        val factoryName = ClassName.get(Factory::class.java)
        val typeKeyName = ClassName.get(TypeKey::class.java)
        val componentBuilderName = ClassName.get(Component.Builder::class.java)
        val interOpName = ClassName.get(InterOp::class.java)

        val superInterfaceName = ParameterizedTypeName.get(factoryName, model.typeName)

        val typeName = model.typeName

        val parameters = model.constructorElement.parameters

        val constructorParameters = parameters.map {
            ClassName.get(it.asType()).box().getInstanceCode(it.isNullable, it.qualifier)
        }.toTypedArray()

        val constructorSignatur = when (parameters.size) {
            0 -> ""
            1 -> "\$L"
            else -> parameters.joinToString(",\n    ", "\n    ", "\n") { "\$L" }
        }

        val invokeMethod = MethodSpec
            .methodBuilder("invoke")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(model.typeName)
            .addParameter(graphName, "graph", Modifier.FINAL)
            .apply {
                if (model.injectorModel != null) {
                    addStatement("\$T instance = new \$T($constructorSignatur)", typeName, typeName, *constructorParameters)
                    addStatement("new \$T().inject(graph, instance)", model.injectorModel.generatedClassName)
                    addStatement("return instance")
                } else {
                    addStatement("return new \$T($constructorSignatur)", typeName, *constructorParameters)
                }
            }
            .build()

        val registerMethod = MethodSpec
            .methodBuilder("register")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(ParameterizedTypeName.get(typeKeyName, typeName))
            .addParameter(componentBuilderName, "builder", Modifier.FINAL)
            .addParameter(ClassName.BOOLEAN, "override", Modifier.FINAL)
            .apply {
                model.scopeAnnotationName?.let { annotation ->
                    val jvmClassMappingName = ClassName.get("kotlin.jvm", "JvmClassMappingKt")
                    val getKClassCode = CodeBlock.of("\$T.getKotlinClass(\$T.class)", jvmClassMappingName, annotation)
                    addStatement("builder.checkComponentQualifier(\$L)", getKClassCode)
                }
            }
            .addStatement("TypeKey<\$L> key = \$L", typeName, typeName.newTypeKeyCode(model.qualifier))
            .addStatement("\$T.${model.scope.name}(builder, key, override, this)", interOpName)
            .addStatement("return key")
            .build()

        val factoryClass = TypeSpec
            .classBuilder(model.generatedClassName)
            .addSuperinterface(superInterfaceName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .generatedAnnotation(configuration.generatedAnnotation)
            .addMethod(invokeMethod)
            .addMethod(registerMethod)
            .build()

        return JavaFile
            .builder(model.generatedClassName.packageName(), factoryClass)
            .build()
    }

}