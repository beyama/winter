package io.jentz.winter

/**
 * Base exception class of all exception thrown by Winter.
 */
open class WinterException(message: String?, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception that is thrown when a component entry wasn't found but was requested in a non-optional way.
 */
class EntryNotFoundException(message: String) : WinterException(message)

/**
 * Exception that is thrown when a cyclic dependency was detected.
 */
class CyclicDependencyException(message: String) : WinterException(message)
