package io.jentz.winter.compiler

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class WinterProcessor : AbstractProcessor() {

    private var configuration: ProcessorConfiguration? = null
    private lateinit var logger: Logger

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        logger = Logger(processingEnv.messager)

        try {
            configuration = ProcessorConfiguration.from(processingEnv)
        } catch (t: Throwable) {
            logger.warn("Skipping annotation processing: ${t.message}")
        }
    }

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf(Inject::class.java.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedOptions(): Set<String> = setOf(
        OPTION_GENERATED_COMPONENT_PACKAGE,
        OPTION_PRINT_SOURCES,
        OPTION_ROOT_SCOPE_ANNOTATION
    )

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val configuration = configuration ?: return true
        val generator = Generator(configuration, logger)

        roundEnv.getElementsAnnotatedWith(Inject::class.java).forEach { element ->
            try {
                generator.addElement(element)
            } catch (t: Throwable) {
                logger.error(element, t)
                return true
            }
        }

        generator.generate()

        return true
    }

}
