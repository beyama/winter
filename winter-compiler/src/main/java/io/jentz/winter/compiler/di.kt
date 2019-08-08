package io.jentz.winter.compiler

import io.jentz.winter.component
import javax.annotation.processing.ProcessingEnvironment

val appComponent = component {

    singleton {
        val environment: ProcessingEnvironment = instance()
        Logger(environment.messager)
    }

    prototype { ProcessorConfiguration.from(instance()) }

    prototype<SourceWriter> {
        val configuration: ProcessorConfiguration = instance()
        SourceFileWriter(configuration.generatedSourcesDirectory)
    }

    prototype { Generator(instance(), instance(), instance()) }

}
