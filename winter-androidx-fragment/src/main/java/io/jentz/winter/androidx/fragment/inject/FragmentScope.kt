package io.jentz.winter.androidx.fragment.inject

import javax.inject.Scope

/**
 * Scope annotation for dependencies with [androidx.fragment.app.Fragment] lifetime.
 */
@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class FragmentScope
