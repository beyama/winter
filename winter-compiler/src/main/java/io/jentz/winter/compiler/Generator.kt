package io.jentz.winter.compiler

import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.element.*

class Generator(
    private val configuration: ProcessorConfiguration,
    private val writer: SourceWriter,
    private val logger: Logger
) {

    private val componentModel = ComponentModel(configuration)

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

    private fun generate() {
        if (componentModel.isEmpty()) return

        generateInjectors()
        generateFactories()
        generateComponent()

    }

    private fun generateInjectors() {
        componentModel.injectors.forEach { (_, injector) ->
            val kCode = injector.generate()
            writer.write(kCode)
        }
    }

    private fun generateFactories() {
        componentModel.factories.forEach { factory ->
            val injectorModel = componentModel.injectors[factory.typeElement]
            val kCode = factory.generate(injectorModel)
            writer.write(kCode)
        }
    }

    private fun generateComponent() {
        val kCode = componentModel.generate()
        writer.write(kCode)
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
