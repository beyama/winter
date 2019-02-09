package io.jentz.winter.compiler

import com.squareup.kotlinpoet.*
import javax.lang.model.element.TypeElement

class InjectorModel(
    private val configuration: ProcessorConfiguration,
    typeElement: TypeElement
) {

    val typeName = typeElement.asClassName()

    val generatedClassName = ClassName(
        typeName.packageName(),
        "${typeName.simpleNames().joinToString("_")}$GENERATED_MEMBERS_INJECTOR_POSTFIX"
    )

    val targets: MutableSet<InjectTargetModel> = mutableSetOf()

    fun generate(): FileSpec =
        FileSpec.builder(generatedClassName.packageName(), generatedClassName.simpleName())
            .addStaticImport(GRAPH_CLASS_NAME.packageName(), GRAPH_CLASS_NAME.simpleName())
            .addType(
                TypeSpec.classBuilder("`${generatedClassName.simpleName()}`")
                    .also {
                        if (configuration.generatedAnnotationAvailable) {
                            it.addAnnotation(generatedAnnotation())
                        } else {
                            it.addKdoc(generatedComment())
                        }
                    }
                    .addSuperinterface(
                        ParameterizedTypeName.get(
                            MEMBERS_INJECTOR_INTERFACE_NAME,
                            typeName
                        )
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
