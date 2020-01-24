package io.jentz.winter.inject

/**
 * Marks a class as eager singleton scoped.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EagerSingleton
