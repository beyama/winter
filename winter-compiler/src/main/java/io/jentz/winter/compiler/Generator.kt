package io.jentz.winter.compiler

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import io.jentz.winter.compiler.generator.ComponentGenerator
import io.jentz.winter.compiler.generator.FactoryGenerator
import io.jentz.winter.compiler.generator.InjectorGenerator
import io.jentz.winter.compiler.model.FactoryModel
import io.jentz.winter.compiler.model.InjectorModel
import io.jentz.winter.inject.InjectConstructor
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

@KotlinPoetMetadataPreview
class Generator(
    private val configuration: ProcessorConfiguration,
    private val writer: SourceWriter,
    private val logger: Logger
) {

    private val factories = mutableListOf<FactoryModel>()
    private val injectors = mutableMapOf<TypeElement, InjectorModel>()

    fun process(roundEnv: RoundEnvironment) {
        factories.clear()
        injectors.clear()

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
        if (factories.isEmpty() && injectors.isEmpty()) {
            return
        }

        if (configuration.generatedComponentPackage != null && factories.isNotEmpty()) {
            generateComponent()
        }

        generateInjectors()
        generateFactories()
    }

    private fun generateComponent() {
        try {
            ComponentGenerator(configuration, factories)
                .generate()
                .also { writer.write(it) }
        } catch (t: Throwable) {
            val stringWriter = StringWriter()
            t.printStackTrace(PrintWriter(stringWriter))
            logger.error(stringWriter.toString())
        }
    }

    private fun generateInjectors() {
        injectors.forEach { (_, injector) ->
            tryWithElement(injector.originatingElement) {
                InjectorGenerator(configuration, injector)
                    .generate()
                    .also { writer.write(it) }
            }
        }
    }

    private fun generateFactories() {
        factories.forEach { factory ->
            tryWithElement(factory.originatingElement) {
                FactoryGenerator(configuration, factory)
                    .generate()
                    .also { writer.write(it) }
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


            val kmClass = runCatching { typeElement.toImmutableKmClass() }.getOrNull()

            InjectorModel(typeElement, superClassWithInjector, kmClass)
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
