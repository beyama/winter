package io.jentz.winter.compiler

import io.jentz.winter.compiler.kotlinbuilder.buildComponent
import io.jentz.winter.compiler.kotlinbuilder.buildFactory
import io.jentz.winter.compiler.kotlinbuilder.buildInjector
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.element.*

class Generator(
    private val configuration: ProcessorConfiguration,
    private val writer: SourceWriter,
    private val logger: Logger
) {

    private val factories = mutableListOf<ServiceModel>()
    private val injectors = mutableMapOf<TypeElement, InjectorModel>()

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
                factories += ServiceModel(executable)
            }
            ElementKind.FIELD -> {
                val field = element as VariableElement
                getOrCreateInjectorModel(element)
                    .targets += InjectTargetModel.FieldInjectTarget(field)
            }
            ElementKind.METHOD -> {
                val method = element as ExecutableElement
                getOrCreateInjectorModel(element)
                    .targets += InjectTargetModel.SetterInjectTarget(method)
            }
            else -> {
                throw IllegalArgumentException(
                    "Inject annotation is only supported for constructor, method or field."
                )
            }
        }
    }

    private fun generate() {
        if (factories.isEmpty() && injectors.isEmpty()) return

        generateInjectors()

        generateFactories()

        if (configuration.generatedComponentPackage != null) {
            generateComponent()
        }
    }

    private fun generateInjectors() {
        injectors.forEach { (_, injector) ->
            val kCode = buildInjector(configuration, injector)
            writer.write(kCode)
        }
    }

    private fun generateFactories() {
        factories.forEach { factory ->
            val kCode = buildFactory(configuration, factory)
            writer.write(kCode)
        }
    }

    private fun generateComponent() {
        val kCode = buildComponent(configuration, factories)
        writer.write(kCode)
    }

    private fun getOrCreateInjectorModel(fieldOrSetter: Element): InjectorModel {
        val typeElement = fieldOrSetter.enclosingElement as? TypeElement
            ?: throw IllegalArgumentException(
                "Enclosing constructor for $fieldOrSetter must be a class"
            )

        return injectors.getOrPut(typeElement) {
            val superClassWithInjector = typeElement
                .selfAndSuperclasses
                .drop(1)
                .firstOrNull { type -> type.enclosedElements.any(Element::isInjectFieldOrMethod) }

            InjectorModel(typeElement, superClassWithInjector)
        }
    }

}
