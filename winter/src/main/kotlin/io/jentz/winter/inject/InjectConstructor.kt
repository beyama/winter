package io.jentz.winter.inject

import kotlin.reflect.KClass

/**
 * Annotation stolen from Toothpick that tells the winter-compiler that the first and only
 * constructor of an annotated class should be treated like it were annotated with
 * [javax.inject.Inject].
 *
 * @param value Allows to register the annotated class with one of its super types.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InjectConstructor(val value: KClass<*> = Nothing::class)
