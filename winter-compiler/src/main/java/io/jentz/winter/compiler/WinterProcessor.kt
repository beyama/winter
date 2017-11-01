package io.jentz.winter.compiler

import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.inject.Named
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.tools.Diagnostic

class WinterProcessor : AbstractProcessor() {

    private val factories = mutableListOf<FactoryModel>()

    private val injectors = mutableMapOf<TypeElement, InjectorModel>()

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(Inject::class.java.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        factories.clear()
        injectors.clear()

        roundEnv.getElementsAnnotatedWith(Inject::class.java).forEach { element ->
            try {
                when (element.kind) {
                    ElementKind.CONSTRUCTOR -> {
                        val execuatable = element as ExecutableElement
                        factories += FactoryModel(execuatable)
                    }
                    ElementKind.FIELD -> {
                        val field = element as VariableElement
                        field.getAnnotationsByType(Named::class.java)
                        getOrCreateInjector(element).targets += InjectTargetModel.FieldInjectTarget(field)
                    }
                    ElementKind.METHOD -> {
                        val method = element as ExecutableElement
                        getOrCreateInjector(element).targets += InjectTargetModel.SetterInjectTarget(method)
                    }
                    else -> {
                        error(element, "Inject annotation is only supported for constructor, method or field.")
                        return true
                    }
                }
            } catch (t: Throwable) {
                error(element, t.message ?: "Unknown error")
                return true
            }
        }

        buildInjectors()

        return true
    }

    private fun buildInjectors() {
        injectors.forEach { (typeElement, injector) ->
            info("injector enclosing ${typeElement.enclosingElement}")

            val kCode = injector.generate()
            info(kCode.toString())
            val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            val file = File(kaptKotlinGeneratedDir)
            kCode.writeTo(file)
            info("Write injector class to ${file.absolutePath}")
        }
    }



    private fun getOrCreateInjector(fieldOrSetter: Element): InjectorModel {
        val typeElement = fieldOrSetter.enclosingElement as? TypeElement ?: throw IllegalArgumentException("Enclosing element for $fieldOrSetter must be a class")
        return injectors.getOrPut(typeElement) { InjectorModel(typeElement) }
    }

    private fun info(element: Element, message: String, vararg args: Any) {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, String.format(message, *args), element)
    }

    private fun info(message: String, vararg args: Any) {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, String.format(message, *args))
    }

    private fun error(element: Element, message: String, vararg args: Any) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, String.format(message, *args), element)
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

}