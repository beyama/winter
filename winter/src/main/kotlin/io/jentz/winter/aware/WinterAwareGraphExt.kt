package io.jentz.winter.aware

import io.jentz.winter.Provider

/**
 * Retrieve a non-optional instance of [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return An instance of [R]
 *
 * @throws io.jentz.winter.EntryNotFoundException
 */
inline fun <reified R : Any> WinterAware.instance(
    qualifier: Any? = null,
    generics: Boolean = false
): R = graph.instance(qualifier, generics)

/**
 * Retrieve an optional instance of [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return An instance of [R] or null if provider doesn't exist.
 */
inline fun <reified R : Any> WinterAware.instanceOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): R? = graph.instanceOrNull(qualifier, generics)

/**
 * Create a [Lazy] that retrieves an instance of `R` when initialized.
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The created [Lazy] instance.
 */
inline fun <reified R : Any> WinterAware.lazyInstance(
    qualifier: Any? = null,
    generics: Boolean = false
): Lazy<R> = lazy { graph.instance<R>(qualifier, generics) }

/**
 * Create a [Lazy] that retrieves an optional instance of `R` when initialized.
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The created [Lazy] instance.
 */
inline fun <reified R : Any> WinterAware.lazyInstanceOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): Lazy<R?> = lazy { graph.instanceOrNull<R>(qualifier, generics) }

/**
 * Retrieve a non-optional provider function that returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The provider that returns [R]
 *
 * @throws io.jentz.winter.EntryNotFoundException
 */
inline fun <reified R : Any> WinterAware.provider(
    qualifier: Any? = null,
    generics: Boolean = false
): Provider<R> = graph.provider(qualifier, generics)

/**
 * Retrieve an optional provider function that returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The provider that returns [R] or null if provider doesn't exist.
 */
inline fun <reified R : Any> WinterAware.providerOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): Provider<R>? = graph.providerOrNull(qualifier, generics)

/**
 * Retrieve all providers of type [R].
 *
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return A [Set] of providers of type `() -> R`.
 */
inline fun <reified R : Any> WinterAware.providersOfType(
    generics: Boolean = false
): Set<Provider<R>> = graph.providersOfType(generics)

/**
 * Retrieve all instances of type [R].
 *
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return A [Set] of instances of type [R].
 */
inline fun <reified R : Any> WinterAware.instancesOfType(
    generics: Boolean = false
): Set<R> = graph.instancesOfType(generics)
