package io.jentz.winter.compiler

import com.squareup.kotlinpoet.*
import javax.lang.model.element.TypeElement

class InjectorModel(val typeElement: TypeElement) {
    companion object {
        val GRAPH_CLASS = ClassName("io.jentz.winter", "Graph")
        val INJECTOR_INTERFACE = ClassName("io.jentz.winter", "MembersInjector")
        val INJECTOR_POSTFIX = "\$\$MembersInjector"
    }

    val typeName = typeElement.asClassName()
    val generatedClassName = ClassName(typeName.packageName(), "${typeName.simpleNames().joinToString("_")}${INJECTOR_POSTFIX}")

    val targets: MutableSet<InjectTargetModel> = mutableSetOf()

    fun generate() = FileSpec.builder(generatedClassName.packageName(), generatedClassName.simpleName())
            .addStaticImport(GRAPH_CLASS.packageName(), GRAPH_CLASS.simpleName())
            .addType(
                    TypeSpec.classBuilder("`${generatedClassName.simpleName()}`")
                            .addSuperinterface(ParameterizedTypeName.get(INJECTOR_INTERFACE, typeName))
                            .addFunction(
                                    FunSpec.builder("injectMembers")
                                            .addModifiers(KModifier.OVERRIDE)
                                            .addParameter("graph", GRAPH_CLASS)
                                            .addParameter("target", typeName)
                                            .also {
                                                targets.forEach { target ->
                                                    it.addCode(target.codeBlock())
                                                }
                                            }
                                            .build()
                            )
                            .build()
            ).build()

}