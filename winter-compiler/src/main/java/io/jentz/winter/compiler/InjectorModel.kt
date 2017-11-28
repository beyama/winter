package io.jentz.winter.compiler

import com.squareup.kotlinpoet.*
import javax.lang.model.element.TypeElement

class InjectorModel(val typeElement: TypeElement) {
    val typeName = typeElement.asClassName()
    val generatedClassName = ClassName(typeName.packageName(), "${typeName.simpleNames().joinToString("_")}${generatedInjectorPostfix}")

    val targets: MutableSet<InjectTargetModel> = mutableSetOf()

    fun generate() = FileSpec.builder(generatedClassName.packageName(), generatedClassName.simpleName())
            .addStaticImport(graphClassName.packageName(), graphClassName.simpleName())
            .addType(
                    TypeSpec.classBuilder("`${generatedClassName.simpleName()}`")
                            .addAnnotation(generatedAnnotation())
                            .addSuperinterface(ParameterizedTypeName.get(injectorInterfaceName, typeName))
                            .addFunction(
                                    FunSpec.builder("injectMembers")
                                            .addModifiers(KModifier.OVERRIDE)
                                            .addParameter("graph", graphClassName)
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