package io.jentz.winter.inject

/**
 * Marks a class as prototype scoped.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Prototype
