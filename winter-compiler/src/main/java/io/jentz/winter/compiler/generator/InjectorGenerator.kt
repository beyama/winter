package io.jentz.winter.compiler.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import io.jentz.winter.Graph
import io.jentz.winter.compiler.ProcessorConfiguration
import io.jentz.winter.compiler.model.InjectTargetModel
import io.jentz.winter.compiler.model.InjectTargetModel.TargetType
import io.jentz.winter.compiler.model.InjectorModel
import io.jentz.winter.inject.MembersInjector

@KotlinPoetMetadataPreview
class InjectorGenerator(
    private val configuration: ProcessorConfiguration,
    private val model: InjectorModel
) {

    fun generate(): FileSpec {
        val membersInjectorName = MembersInjector::class.asClassName()

        val superInterfaceName =  membersInjectorName.parameterizedBy(model.typeName)

        val graphName = Graph::class.asClassName()

        val injectMethod = FunSpec.builder("inject")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .addParameter("graph", graphName)
            .addParameter("target", model.typeName)
            .addSuperclassInjector(model.superclassInjectorClassName)
            .addTargets(model.targets)
            .build()

        val injectorClass = TypeSpec.classBuilder(model.generatedClassName)
            .addSuperinterface(superInterfaceName)
            .addModifiers(KModifier.PUBLIC)
            .generatedAnnotation(configuration.generatedAnnotation)
            .addOriginatingElement(model.originatingElement)
            .addFunction(injectMethod)
            .build()

        return FileSpec.get(model.generatedClassName.packageName, injectorClass)
    }

    private fun FunSpec.Builder.addSuperclassInjector(
        superclassInjectorClassName: ClassName?
    ): FunSpec.Builder {
        superclassInjectorClassName?.let { className ->
            addStatement("%T().inject(graph, target)", className)
        }
        return this
    }

    private fun FunSpec.Builder.addTargets(targets: Iterable<InjectTargetModel>): FunSpec.Builder {
        targets.forEach { addTarget(it) }
        return this
    }

    private fun FunSpec.Builder.addTarget(targetModel: InjectTargetModel) {
        val getInstance = targetModel.targetTypeName.getInstanceCode(targetModel.isNullable, targetModel.qualifier)

        when (targetModel.targetType) {
            TargetType.Field, TargetType.Property -> {
                addCode("target.${targetModel.targetName} = %L\n", getInstance)
            }
            TargetType.Method -> {
                addCode("target.${targetModel.targetName}(%L)\n", getInstance)
            }
        }
    }

}
