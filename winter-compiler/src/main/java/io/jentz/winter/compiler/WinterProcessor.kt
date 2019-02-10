package io.jentz.winter.compiler

import io.jentz.winter.Injector
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class WinterProcessor : AbstractProcessor() {

    private val injector = Injector()
    private val logger: Logger by injector.instance()
    private val generatorProvider: () -> Generator by injector.provider()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        injector.inject(appComponent.init { constant(processingEnv) })
    }

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf(Inject::class.java.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedOptions(): Set<String> = setOf(
        OPTION_GENERATED_COMPONENT_PACKAGE,
        OPTION_ROOT_SCOPE_ANNOTATION
    )

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {

        try {
            generatorProvider().process(roundEnv)
        } catch (t: Throwable) {
            logger.warn("Skipping annotation processing: ${t.message}")
        }

        return true
    }

}
