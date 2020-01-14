package io.jentz.winter.compiler

import io.jentz.winter.compiler.generator.InjectorGenerator
import io.jentz.winter.compiler.kotlinbuilder.buildFactory
import io.jentz.winter.compiler.model.FactoryModel
import io.jentz.winter.compiler.model.InjectorModel
import io.jentz.winter.inject.InjectConstructor
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

class Generator(
    private val configuration: ProcessorConfiguration,
    private val writer: SourceWriter,
    private val logger: Logger
) {

    private val factories = mutableListOf<FactoryModel>()
    private val injectors = mutableMapOf<TypeElement, InjectorModel>()

    fun process(roundEnv: RoundEnvironment) {
        processInjectConstructorAnnotatedElements(roundEnv)
        processInjectAnnotatedElements(roundEnv)

        generate()
    }

    private fun processInjectConstructorAnnotatedElements(roundEnv: RoundEnvironment) {
        ElementFilter
            .typesIn(roundEnv.getElementsAnnotatedWith(InjectConstructor::class.java))
            .forEach { type ->
                tryWithElement(type) {
                    factories += FactoryModel.forInjectConstructorAnnotatedClass(it)
                }
            }
    }

    private fun processInjectAnnotatedElements(roundEnv: RoundEnvironment) {
        roundEnv
            .getElementsAnnotatedWith(Inject::class.java)
            .forEach { element ->
                tryWithElement(element) {
                    addElement(it)
                }
            }
    }

    private fun addElement(element: Element) {
        when (element.kind) {
            ElementKind.CONSTRUCTOR -> {
                val executable = element as ExecutableElement
                factories += FactoryModel.forInjectAnnotatedConstructor(executable)
            }
            ElementKind.FIELD, ElementKind.METHOD -> {
                getOrCreateInjectorModel(element).addTarget(element)
            }
            else -> {
                throw IllegalArgumentException(
                    "Inject annotation is only supported for constructors, methods or fields."
                )
            }
        }
    }

    private fun generate() {
        generateInjectors()
        generateFactories()
    }

    private fun generateInjectors() {
        injectors.forEach { (_, injector) ->
            tryWithElement(injector.originatingElement) {
                val kCode = InjectorGenerator(configuration, injector, logger).generate()
                writer.write(kCode)
            }
        }
    }

    private fun generateFactories() {
        factories.forEach { factory ->
            tryWithElement(factory.originatingElement) {
                val kCode = buildFactory(configuration, factory)
                writer.write(kCode)
            }
        }
    }

    private fun getOrCreateInjectorModel(fieldOrSetter: Element): InjectorModel {
        val typeElement = fieldOrSetter.enclosingElement as? TypeElement
            ?: throw IllegalArgumentException(
                "Enclosing element of Inject annotated field or setter must be a class."
            )

        return injectors.getOrPut(typeElement) {
            val superClassWithInjector = typeElement
                .selfAndSuperclasses
                .drop(1)
                .firstOrNull { type -> type.enclosedElements.any(Element::isInjectFieldOrMethod) }

            val kotlinMetadata = KotlinMetadata.fromTypeElement(typeElement)

            InjectorModel(typeElement, superClassWithInjector, kotlinMetadata)
        }
    }

    private inline fun <T : Element> tryWithElement(element: T, block: (T) -> Unit) {
        try {
            block(element)
        } catch (t: Throwable) {
            logger.error(element, t)
        }
    }

}
