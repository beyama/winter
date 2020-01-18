package io.jentz.winter.inject

import kotlin.reflect.KClass

/**
 * Change the factory type to one of the super types of the annotated classes.
 *
 * @param value Register the annotated class with one of its super types.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class FactoryType(val value: KClass<*>)
