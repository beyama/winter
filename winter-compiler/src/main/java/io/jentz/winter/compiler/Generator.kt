package io.jentz.winter.compiler

import io.jentz.winter.compiler.kotlinbuilder.buildComponent
import io.jentz.winter.compiler.kotlinbuilder.buildInjector
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

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
        generateComponent()

    }

    private fun generateInjectors() {
        injectors.forEach { (_, injector) ->
            val kCode = buildInjector(configuration, injector)
            writer.write(kCode)
        }
    }

    private fun generateComponent() {
        val kCode = buildComponent(configuration, factories, injectors)
        writer.write(kCode)
    }

    private fun getOrCreateInjectorModel(fieldOrSetter: Element): InjectorModel {
        val typeElement = fieldOrSetter.enclosingElement as? TypeElement
            ?: throw IllegalArgumentException(
                "Enclosing constructor for $fieldOrSetter must be a class"
            )

        return injectors.getOrPut(typeElement) {
            val superClasses = generateSequence(typeElement) {
                if (it.superclass.kind == TypeKind.DECLARED) {
                    (it.superclass as DeclaredType).asElement() as TypeElement
                } else {
                    null
                }
            }
            val superClassWithInjector = superClasses
                .filter { it != typeElement }
                .firstOrNull { type ->
                    type.enclosedElements.any {
                        it.getAnnotation(Inject::class.java) != null &&
                                (it.kind == ElementKind.FIELD || it.kind == ElementKind.METHOD)
                    }
                }

            InjectorModel(typeElement, superClassWithInjector)
        }
    }

}
