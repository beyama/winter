package io.jentz.winter.compiler

import io.jentz.winter.component
import javax.annotation.processing.ProcessingEnvironment

val appComponent = component {

    singleton { Logger(instance<ProcessingEnvironment>().messager) }

    prototype { instance<ProcessingEnvironment>().filer }

    prototype { ProcessorConfiguration.from(instance()) }

    prototype { Generator(instance(), instance(), instance()) }

}
