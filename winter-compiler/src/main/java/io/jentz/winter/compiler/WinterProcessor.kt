package io.jentz.winter.compiler

import io.jentz.winter.WinterException
import io.jentz.winter.delegate.inject
import io.jentz.winter.delegate.injectProvider
import io.jentz.winter.inject.InjectConstructor
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class WinterProcessor : AbstractProcessor() {

    private val logger: Logger by inject()
    private val configuration: ProcessorConfiguration by inject()
    private val generatorProvider: () -> Generator by injectProvider()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        appComponent.createGraph {
            constant(processingEnv)
        }.inject(this)
    }

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf(Inject::class.java.canonicalName, InjectConstructor::class.java.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedOptions(): Set<String> = setOf(
        OPTION_GENERATED_COMPONENT_PACKAGE,
        OPTION_ROOT_SCOPE_ANNOTATION,
        if (configuration.generatedComponentPackage == null) {
            "org.gradle.annotation.processing.isolating"
        } else {
            "org.gradle.annotation.processing.aggregating"
        }
    )

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {

        try {
            generatorProvider().process(roundEnv)
        } catch (e: WinterException) {
            logger.warn("Skipping annotation processing: ${e.cause?.message}")
        } catch (t: Throwable) {
            logger.warn("Skipping annotation processing: ${t.message}")
        }

        return true
    }

}
