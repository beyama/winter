package io.jentz.winter.androidx.inject

import io.jentz.winter.inject.Scope

/**
 * Scope annotation for dependencies with [android.app.Activity] lifetime.
 */
@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class ActivityScope
