package io.jentz.winter.compiler.generator

import com.squareup.javapoet.*
import io.jentz.winter.Graph
import io.jentz.winter.compiler.ProcessorConfiguration
import io.jentz.winter.compiler.model.FieldInjectTarget
import io.jentz.winter.compiler.model.InjectTargetModel
import io.jentz.winter.compiler.model.InjectorModel
import io.jentz.winter.compiler.model.SetterInjectTarget
import io.jentz.winter.inject.MembersInjector
import kotlinx.metadata.jvm.setterSignature
import javax.lang.model.element.Modifier

class InjectorGenerator(
    private val configuration: ProcessorConfiguration,
    private val model: InjectorModel
) {

    fun generate(): JavaFile {
        val membersInjectorName = ClassName.get(MembersInjector::class.java)

        val superInterfaceName = ParameterizedTypeName.get(membersInjectorName, model.typeName)

        val graphName = ClassName.get(Graph::class.java)

        val injectMethod = MethodSpec
            .methodBuilder("inject")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(graphName, "graph", Modifier.FINAL)
            .addParameter(model.typeName, "target", Modifier.FINAL)
            .addSuperclassInjector(model.superclassInjectorClassName)
            .addTargets(model.targets)
            .build()

        val injectorClass = TypeSpec
            .classBuilder(model.generatedClassName)
            .addSuperinterface(superInterfaceName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .generatedAnnotation(configuration.generatedAnnotation)
            .addOriginatingElement(model.originatingElement)
            .addMethod(injectMethod)
            .build()

        return JavaFile
            .builder(model.generatedClassName.packageName(), injectorClass)
            .build()
    }

    private fun MethodSpec.Builder.addSuperclassInjector(
        superclassInjectorClassName: ClassName?
    ): MethodSpec.Builder {
        superclassInjectorClassName?.let { className ->
            addStatement("new \$T().inject(graph, target)", className)
        }
        return this
    }

    private fun MethodSpec.Builder.addTargets(targets: Iterable<InjectTargetModel>): MethodSpec.Builder {
        targets.forEach { addTarget(it) }
        return this
    }

    private fun MethodSpec.Builder.addTarget(targetModel: InjectTargetModel) {
        val targetTypeName = ClassName.get(targetModel.variableElement.asType()).box()

        val getInstance = targetTypeName.getInstanceCode(targetModel.isNullable, targetModel.qualifier)

        when (targetModel) {
            is FieldInjectTarget -> {
                val setterSignature = targetModel.kmProperty?.setterSignature?.name
                if (setterSignature != null) {
                    addCode("target.$setterSignature(\$L);\n", getInstance)
                } else {
                    val fieldName = targetModel.variableElement.simpleName
                    addCode("target.$fieldName = \$L;\n", getInstance)
                }
            }
            is SetterInjectTarget -> {
                val setterName = targetModel.originatingElement.simpleName
                addCode("target.$setterName(\$L);\n", getInstance)
            }
        }
    }

}
