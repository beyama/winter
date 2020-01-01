package io.jentz.winter.inject

/**
 * Annotation stolen from Toothpick that tells the winter-compiler that the first and only
 * constructor of an annotated class should be treated like it were annotated with
 * [javax.inject.Inject].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InjectConstructor
