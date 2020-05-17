package io.jentz.winter.compiler

import com.squareup.kotlinpoet.ClassName
import javax.annotation.processing.ProcessingEnvironment

data class ProcessorConfiguration(
    val generatedComponentPackage: String?,
    val generatedAnnotation: ClassName?
) {

    companion object {

        private val generatedAnnotations = listOf(
            GENERATED_ANNOTATION_JDK9_INTERFACE_NAME,
            GENERATED_ANNOTATION_LEGACY_INTERFACE_NAME
        )

        fun from(processingEnv: ProcessingEnvironment): ProcessorConfiguration {
            val options = processingEnv.options

            val generatedComponentPackage = options[OPTION_GENERATED_COMPONENT_PACKAGE]
                .takeUnless { it.isNullOrBlank() }

            // Android's API jar doesn't include a Generated annotation so we check the
            // availability here
            val generatedAnnotation = generatedAnnotations.find {
                processingEnv.elementUtils.getTypeElement(it.canonicalName) != null
            }

            return ProcessorConfiguration(generatedComponentPackage, generatedAnnotation)

        }

    }

}
