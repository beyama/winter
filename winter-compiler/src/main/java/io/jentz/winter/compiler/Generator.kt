package io.jentz.winter.compiler

import io.jentz.winter.compiler.kotlinbuilder.buildComponent
import io.jentz.winter.compiler.kotlinbuilder.buildInjector
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.element.*

class Generator(
    private val configuration: ProcessorConfiguration,
    private val writer: SourceWriter,
    private val logger: Logger
) {

    private val componentModel = ComponentModel()

    fun process(roundEnv: RoundEnvironment) {
        roundEnv.getElementsAnnotatedWith(Inject::class.java).forEach { element ->
            try {
                addElement(element)
            } catch (t: Throwable) {
                logger.error(element, t)
                return
            }
        }

        generate()
    }

    private fun addElement(element: Element) {
        when (element.kind) {
            ElementKind.CONSTRUCTOR -> {
                val executable = element as ExecutableElement
                componentModel.factories += ServiceModel(executable)
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

    private fun generate() {
        if (componentModel.isEmpty()) return

        generateInjectors()
        generateComponent()

    }

    private fun generateInjectors() {
        componentModel.injectors.forEach { (_, injector) ->
            val kCode = buildInjector(configuration, injector)
            writer.write(kCode)
        }
    }

    private fun generateComponent() {
        val kCode = buildComponent(configuration, componentModel)
        writer.write(kCode)
    }

    private fun getOrCreateInjectorModel(fieldOrSetter: Element): InjectorModel {
        val typeElement = fieldOrSetter.enclosingElement as? TypeElement
            ?: throw IllegalArgumentException(
                "Enclosing constructor for $fieldOrSetter must be a class"
            )
        return componentModel.injectors.getOrPut(typeElement) {
            InjectorModel(typeElement)
        }
    }

}
