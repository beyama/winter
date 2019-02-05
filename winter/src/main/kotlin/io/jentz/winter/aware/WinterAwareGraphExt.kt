package io.jentz.winter.aware

import io.jentz.winter.Factory
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
 * Retrieve a factory of type `(A) -> R` and apply [argument] to it.
 *
 * @param argument The argument for the factory to retrieve.
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The result of applying [argument] to the retrieved factory.
 *
 * @throws io.jentz.winter.EntryNotFoundException
 */
inline fun <reified A, reified R : Any> WinterAware.instance(
    argument: A,
    qualifier: Any? = null,
    generics: Boolean = false
): R = graph.instance(argument, qualifier, generics)

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
 * Retrieve an optional factory of type `(A) -> R` and apply [argument] to it.
 *
 * @param argument The argument for the factory to retrieve.
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The result of applying [argument] to the retrieved factory or null if factory doesn't
exist.
 *
 */
inline fun <reified A, reified R : Any> WinterAware.instanceOrNull(
    argument: A,
    qualifier: Any? = null,
    generics: Boolean = false
): R? = graph.instanceOrNull(argument, qualifier, generics)

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
 * Create a [Lazy] that retrieves a factory of type `(A) -> R` and applies [argument]
 * to it when initialized.
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The created [Lazy] instance.
 */
inline fun <reified A, reified R : Any> WinterAware.lazyInstance(
    argument: A,
    qualifier: Any? = null,
    generics: Boolean = false
): Lazy<R> = lazy { graph.instance<A, R>(argument, qualifier, generics) }

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
 * Create a [Lazy] that retrieves an optional factory of type `(A) -> R` and
 * applies [argument] to it when initialized.
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The created [Lazy] instance.
 */
inline fun <reified A, reified R : Any> WinterAware.lazyInstanceOrNull(
    argument: A,
    qualifier: Any? = null,
    generics: Boolean = false
): Lazy<R?> = lazy { graph.instanceOrNull<A, R>(argument, qualifier, generics) }

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
 * Retrieve a factory of type `(A) -> R` and create and return a
 * [provider][Provider] that applies the given [argument] to the factory when called.
 *
 * @param argument The argument for the factory to retrieve.
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The provider function.
 *
 * @throws io.jentz.winter.EntryNotFoundException
 */
inline fun <reified A, reified R : Any> WinterAware.provider(
    argument: A,
    qualifier: Any? = null,
    generics: Boolean = false
): Provider<R> = graph.provider(argument, qualifier, generics)

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
 * Retrieve an optional factory of type `(A) -> R` and create and return a
 * [provider][Provider] that applies the given [argument] to the factory when called.
 *
 * @param argument The argument for the factory to retrieve.
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The provider function or null if factory doesn't exist.
 */
inline fun <reified A, reified R : Any> WinterAware.providerOrNull(
    argument: A,
    qualifier: Any? = null,
    generics: Boolean = false
): Provider<R>? = graph.providerOrNull(argument, qualifier, generics)

/**
 * Retrieve a non-optional factory function that takes an argument of type [A] and returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The factory that takes [A] and returns [R]
 *
 * @throws io.jentz.winter.EntryNotFoundException
 */
inline fun <reified A : Any, reified R : Any> WinterAware.factory(
    qualifier: Any? = null,
    generics: Boolean = false
): Factory<A, R> = graph.factory(qualifier, generics)

/**
 * Retrieve an optional factory function that takes an argument of type [A] and returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The factory that takes [A] and returns [R] or null if factory provider doesn't exist.
 *
 * @throws io.jentz.winter.EntryNotFoundException
 */
inline fun <reified A : Any, reified R : Any> WinterAware.factoryOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): Factory<A, R>? = graph.factoryOrNull(qualifier, generics)

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
