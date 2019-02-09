package io.jentz.winter.compiler

import com.squareup.kotlinpoet.FileSpec
import java.io.File
import javax.lang.model.element.*

class Generator(
    private val configuration: ProcessorConfiguration,
    private val logger: Logger
) {

    private val componentModel = ComponentModel(configuration)

    fun addElement(element: Element) {
        when (element.kind) {
            ElementKind.CONSTRUCTOR -> {
                val executable = element as ExecutableElement
                componentModel.factories += FactoryModel(configuration, executable)
            }
            ElementKind.FIELD -> {
                val field = element as VariableElement
                getOrCreateInjectorModel(element).targets +=
                    InjectTargetModel.FieldInjectTarget(field)
            }
            ElementKind.METHOD -> {
                val method = element as ExecutableElement
                getOrCreateInjectorModel(element).targets +=
                    InjectTargetModel.SetterInjectTarget(method)
            }
            else -> {
                throw IllegalArgumentException(
                    "Inject annotation is only supported for constructor, method or field."
                )
            }
        }
    }

    fun generate() {
        if (componentModel.isEmpty()) return

        generateInjectors()
        generateFactories()
        generateComponent()

    }

    private fun generateInjectors() {
        componentModel.injectors.forEach { (_, injector) ->
            val kCode = injector.generate()
            write(kCode)
            print(kCode)
        }
    }

    private fun generateFactories() {
        componentModel.factories.forEach { factory ->
            val injectorModel = componentModel.injectors[factory.typeElement]
            val kCode = factory.generate(injectorModel)
            write(kCode)
            print(kCode)
        }
    }

    private fun generateComponent() {
        val kCode = componentModel.generate()
        write(kCode)
        print(kCode)
    }

    private fun write(fileSpec: FileSpec) {
        val file = File(configuration.generatedSourcesDirectory)
        fileSpec.writeTo(file)
    }

    private fun print(fileSpec: FileSpec) {
        if (configuration.printSources) logger.info(fileSpec.toString())
    }

    private fun getOrCreateInjectorModel(fieldOrSetter: Element): InjectorModel {
        val typeElement = fieldOrSetter.enclosingElement as? TypeElement
            ?: throw IllegalArgumentException(
                "Enclosing constructor for $fieldOrSetter must be a class"
            )
        return componentModel.injectors.getOrPut(typeElement) {
            InjectorModel(configuration, typeElement)
        }
    }

}
