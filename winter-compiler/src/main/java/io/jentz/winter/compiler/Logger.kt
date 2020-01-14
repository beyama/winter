package io.jentz.winter.compiler

import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic

class Logger(private val messager: Messager) {

    fun info(message: String) {
        messager.printMessage(Diagnostic.Kind.NOTE, message)
    }

    fun warn(message: String) {
        messager.printMessage(Diagnostic.Kind.WARNING, message)
    }

    fun error(element: Element, throwable: Throwable) {
        error(element, throwable.message ?: "Unknown error")
    }

    fun error(element: Element, message: String) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element)
    }

    fun error(message: String) {
        messager.printMessage(Diagnostic.Kind.ERROR, message)
    }

}
