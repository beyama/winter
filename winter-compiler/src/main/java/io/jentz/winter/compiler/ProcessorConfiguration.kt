package io.jentz.winter.compiler

import javax.annotation.processing.ProcessingEnvironment

data class ProcessorConfiguration(
        val generatedComponentPackage: String,
        val generatedSourcesDirectory: String,
        val generatedAnnotationAvailable: Boolean,
        val rootScopeAnnotation: String
) {

    companion object {

        fun from(processingEnv: ProcessingEnvironment): ProcessorConfiguration {

            val options = processingEnv.options

            val generatedSourcesDirectory = options[OPTION_KAPT_KOTLIN_GENERATED]
                    ?: throw IllegalArgumentException(
                            "Kapt generated sources directory is not set."
                    )

            val generatedComponentPackage = options[OPTION_GENERATED_COMPONENT_PACKAGE]
                    .takeUnless { it.isNullOrBlank() }
                    ?: throw IllegalArgumentException(
                            "Package to generate component to is not configured. " +
                                    "Set option `$OPTION_GENERATED_COMPONENT_PACKAGE`."
                    )

            val rootScopeAnnotation = options[OPTION_ROOT_SCOPE_ANNOTATION]
                    .takeUnless { it.isNullOrBlank() }
                    ?: JAVAX_SINGLETON_ANNOTATION_NAME

            // Android's API jar doesn't include javax.annotation.Generated so we check the
            // availability here
            val generatedAnnotationAvailable = processingEnv.elementUtils
                    .getTypeElement(GENERATED_ANNOTATION_NAME.canonicalName) != null

            return ProcessorConfiguration(
                    generatedComponentPackage = generatedComponentPackage,
                    generatedSourcesDirectory = generatedSourcesDirectory,
                    generatedAnnotationAvailable = generatedAnnotationAvailable,
                    rootScopeAnnotation = rootScopeAnnotation
            )

        }

    }

}
