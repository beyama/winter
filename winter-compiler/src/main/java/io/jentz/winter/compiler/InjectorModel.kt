package io.jentz.winter.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import javax.lang.model.element.TypeElement

class InjectorModel(
    private val configuration: ProcessorConfiguration,
    typeElement: TypeElement
) {

    val typeName = typeElement.asClassName()

    val generatedClassName = ClassName(
        typeName.packageName,
        "WinterMembersInjector_${typeName.simpleNames.joinToString("_")}"
    )

    val targets: MutableSet<InjectTargetModel> = mutableSetOf()

    fun generate(): FileSpec =
        FileSpec.builder(generatedClassName.packageName, generatedClassName.simpleName)
            .addImport(GRAPH_CLASS_NAME.packageName, GRAPH_CLASS_NAME.simpleName)
            .addType(
                TypeSpec.classBuilder(generatedClassName)
                    .also {
                        if (configuration.generatedAnnotationAvailable) {
                            it.addAnnotation(generatedAnnotation())
                        } else {
                            it.addKdoc(generatedComment())
                        }
                    }
                    .addSuperinterface(
                        MEMBERS_INJECTOR_INTERFACE_NAME.plusParameter(typeName)
                    )
                    .addFunction(
                        FunSpec.builder("injectMembers")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("graph", GRAPH_CLASS_NAME)
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
