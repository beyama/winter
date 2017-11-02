package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
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
        buildFactories()
        buildRegistry()

        return true
    }

    private fun buildInjectors() {
        injectors.forEach { (_, injector) ->
            info("Create injector for ${injector.typeName}")
            val kCode = injector.generate()
            info(kCode.toString())
            write(kCode)
        }
    }

    private fun buildFactories() {
        factories.forEach { factory ->
            info("Create factory for ${factory.typeName}")
            val kCode = factory.generate(injectors[factory.typeElement])
            info(kCode.toString())
            write(kCode)
        }
    }

    private fun buildRegistry() {
        val graphClass = ClassName("io.jentz.winter", "Graph")

        val block = CodeBlock.builder()
                .beginControlFlow("component")
                .also { block ->
                    factories.forEach {
                        block.add("singleton<%T> { `%T`().instance(this) }\n", it.typeName, it.generatedClassName)
                    }
                }
                .endControlFlow()
                .build()

        val file = FileSpec.builder("io.jentz.winter", "Registry")
                .addProperty(
                        PropertySpec.builder("generatedComponent", graphClass)
                                .initializer(block)
                                .build()
                )

        info(file.build().toString())
    }

    private fun write(fileSpec: FileSpec) {
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val file = File(kaptKotlinGeneratedDir)
        fileSpec.writeTo(file)
    }

    private fun getOrCreateInjector(fieldOrSetter: Element): InjectorModel {
        val typeElement = fieldOrSetter.enclosingElement as? TypeElement ?: throw IllegalArgumentException("Enclosing constructor for $fieldOrSetter must be a class")
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