package io.jentz.winter.inject

import javax.inject.Scope

/**
 * Scope annotation for application wide available dependencies and default qualifier for root
 * [components][io.jentz.winter.Component].
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ApplicationScope
