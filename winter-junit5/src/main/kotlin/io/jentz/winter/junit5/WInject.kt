package io.jentz.winter.junit5

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

/**
 * This annotation can be used to inject values into JUnit5 methods and constructors by using
 * the test graph to resolve them.
 *
 * The problem with Javax Inject is, that it does not allow value parameter targets.
 *
 * Example in a test using one of the Winter JUnit5 extensions:
 * ```
 * @Test fun myTest(@WInject service: Service) {
 *   // do something with service
 * }
 *
 * ```
 *
 */
@Target(VALUE_PARAMETER)
@Retention(RUNTIME)
@MustBeDocumented
annotation class WInject(val qualifier: String = "")
