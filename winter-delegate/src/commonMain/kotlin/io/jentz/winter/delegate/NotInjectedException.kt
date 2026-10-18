package io.jentz.winter.delegate

/**
 * Thrown by [InjectedProperty.value] when the property isn't injected yet.
 *
 * This is [UninitializedPropertyAccessException] on the JVM, the Kotlin/Native and Kotlin/Wasm
 * standard libraries don't make it public.
 */
internal expect class NotInjectedException(message: String?) : RuntimeException
