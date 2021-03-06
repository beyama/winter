package io.jentz.winter.compiler

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import io.jentz.winter.Graph
import io.jentz.winter.WinterApplication
import javax.annotation.processing.ProcessingEnvironment

@KotlinPoetMetadataPreview
object DI : WinterApplication() {

    init {
        component {
            singleton { Logger(instance<ProcessingEnvironment>().messager) }

            prototype { instance<ProcessingEnvironment>().filer }

            prototype { instance<ProcessingEnvironment>().elementUtils }

            prototype { instance<ProcessingEnvironment>().typeUtils }

            prototype { ProcessorConfiguration.from(instance()) }

            prototype { TypeUtils(instance(), instance()) }

            prototype { Generator(instance(), instance(), instance()) }

            singleton<SourceWriter> { FilerSourceWriter(instance()) }
        }

        injectionAdapter = object : InjectionAdapter {
            override fun get(instance: Any): Graph? {
                if (instance is ProcessingEnvironment) {
                    closeGraphIfOpen() // in testing
                    return openGraph { constant(instance) }
                }
                return graphOrNull
            }
        }
    }




}