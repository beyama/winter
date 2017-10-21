package io.jentz.winter

open class WinterException(message: String?, cause: Throwable? = null) : Exception(message, cause)
class EntryNotFoundException(message: String) : WinterException(message)
class CyclicDependencyException(message: String) : WinterException(message)
