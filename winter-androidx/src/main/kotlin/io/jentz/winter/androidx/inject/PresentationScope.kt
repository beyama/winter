package io.jentz.winter.androidx.inject

import javax.inject.Scope

/**
 * Scope annotation for dependencies that outlive Activity recreations and are destroyed when an
 * [android.app.Activity] finishes.
 */
@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class PresentationScope
