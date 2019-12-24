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
        val environment: ProcessingEnvironment = instance()
        SourceFileWriter(environment.filer)
    }

    prototype { Generator(instance(), instance(), instance()) }

}
