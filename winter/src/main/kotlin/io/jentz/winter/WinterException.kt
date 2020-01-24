package io.jentz.winter

/**
 * Base exception class of all exception thrown by Winter.
 */
open class WinterException(message: String?, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception that is thrown when a component entry or graph was not found but was requested as
 * non-optional.
 */
class EntryNotFoundException(val key: TypeKey<*>, message: String) : WinterException(message)

/**
 * Exception that is thrown when an error occurs during dependency resolution.
 */
class DependencyResolutionException(
    val key: TypeKey<*>,
    message: String,
    cause: Throwable? = null
) : WinterException(message, cause)

/**
 * Exception that is thrown when a cyclic dependency was detected.
 */
class CyclicDependencyException(
    val key: TypeKey<*>,
    message: String,
    cause: Throwable? = null
) : WinterException(message, cause)
