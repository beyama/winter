package io.jentz.winter.compiler

import com.squareup.kotlinpoet.*
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class FactoryModel(val constructor: ExecutableElement) {
    companion object {
        val FACTORY_INTERFACE = ClassName("io.jentz.winter", "Factory")
        val FACTORY_POSTFIX = "\$\$Factory"
    }

    val typeElement = (constructor.enclosingElement as TypeElement)
    val typeName = typeElement.asClassName()
    val generatedClassName = ClassName(typeName.packageName(), "${typeName.simpleNames().joinToString("_")}$FACTORY_POSTFIX")

    fun generate(injectorModel: InjectorModel?) = FileSpec.builder(generatedClassName.packageName(), generatedClassName.simpleName())
            .addType(
                    TypeSpec.classBuilder("`${generatedClassName.simpleName()}`")
                            .addSuperinterface(ParameterizedTypeName.get(FACTORY_INTERFACE, typeName))
                            .addFunction(
                                    FunSpec.builder("createInstance")
                                            .addModifiers(KModifier.OVERRIDE)
                                            .addParameter("graph", InjectorModel.GRAPH_CLASS)
                                            .returns(typeName)
                                            .also { block ->
                                                val createInstance = CodeBlock.builder()
                                                        .add("%T(", typeName)
                                                        .also { createInstanceBlock ->
                                                            val params = constructor.parameters.map { parameter ->
                                                                val type = parameter.asType().asTypeName()
                                                                /**
                                                                 * TODO: Is there a better solution and does that work for all java.lang.* classes?
                                                                 * Without this we get errors like:
                                                                 * "Type mismatch: inferred type is java.lang.String but kotlin.String was expected"
                                                                 */
                                                                val instanceType = if (type.toString().startsWith("java.lang.")) {
                                                                    ClassName("", type.toString().removePrefix("java.lang."))
                                                                } else {
                                                                    type
                                                                }
                                                                CodeBlock.of("graph.instance<%T>()", instanceType)
                                                            }.joinToString(", ")

                                                            if (params.isNotEmpty()) {
                                                                createInstanceBlock.add(params)
                                                            }
                                                        }
                                                        .add(")")
                                                        .build()
                                                if (injectorModel == null) {
                                                    block.addCode(
                                                            CodeBlock.builder()
                                                                    .add("return ")
                                                                    .add(createInstance)
                                                                    .add("\n")
                                                                    .build()
                                                    )
                                                } else {
                                                    block.addCode(
                                                            CodeBlock.builder()
                                                                    .add("val instance = ")
                                                                    .add(createInstance)
                                                                    .add("\n")
                                                                    .add("`%T`().injectMembers(graph, instance)\n", injectorModel.generatedClassName)
                                                                    .add("return instance\n")
                                                                    .build()
                                                    )
                                                }
                                            }
                                            .build()
                            )
                            .build()
            )
            .build()

}