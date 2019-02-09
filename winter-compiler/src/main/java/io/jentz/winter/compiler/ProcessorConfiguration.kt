package io.jentz.winter.compiler

import javax.annotation.processing.ProcessingEnvironment

data class ProcessorConfiguration(
    val generatedComponentPackage: String,
    val generatedSourcesDirectory: String,
    val generatedAnnotationAvailable: Boolean,
    val rootScopeAnnotation: String,
    val printSources: Boolean
) {

    companion object {

        fun from(processingEnv: ProcessingEnvironment): ProcessorConfiguration {

            val generatedSourcesDirectory = processingEnv.options[OPTION_KAPT_KOTLIN_GENERATED]
                ?: throw IllegalArgumentException("Kapt generated sources directory is not set.")

            val generatedComponentPackage = processingEnv
                .options[OPTION_GENERATED_COMPONENT_PACKAGE]
                .takeUnless { it.isNullOrBlank() }
                ?: throw IllegalArgumentException(
                    "Package to generate component to is not configured. " +
                            "Set option `$OPTION_GENERATED_COMPONENT_PACKAGE`."
                )

            val printSources = processingEnv.options[OPTION_PRINT_SOURCES] == "true"

            // Android's API jar doesn't include javax.annotation.Generated so we check the
            // availability here
            val generatedAnnotationAvailable = processingEnv.elementUtils
                .getTypeElement(GENERATED_ANNOTATION_NAME.canonicalName) != null

            return ProcessorConfiguration(
                generatedComponentPackage = generatedComponentPackage,
                generatedSourcesDirectory = generatedSourcesDirectory,
                generatedAnnotationAvailable = generatedAnnotationAvailable,
                rootScopeAnnotation = "",
                printSources = printSources
            )

        }

    }

}
