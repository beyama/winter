/**
 * GENERATED FILE - DO NOT EDIT!
 *
 * To change the content of this file edit inject_extension.erb and run 'ruby generate_inject_extensions.rb'.
 */
package io.jentz.winter.android

import android.view.View
import io.jentz.winter.EntryNotFoundException
import io.jentz.winter.Factory
import io.jentz.winter.Graph
import io.jentz.winter.Injection
import io.jentz.winter.Provider


/**
 * Return the graph associated with `this`.
 * This is sugar for calling "Injection.getGraph(this)".
 */
inline val View.dependencyGraph: Graph get() = Injection.getGraph(this)

/**
 * Retrieve a non-optional instance of [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return An instance of [R]
 *
 * @throws EntryNotFoundException
 */
inline fun <reified R : Any> View.instance(
        qualifier: Any? = null,
        generics: Boolean = false
): R = dependencyGraph.instance(qualifier, generics)

/**
 * Retrieve a factory of type `(A) -> R` and apply [argument] to it.
 *
 * @param argument The argument for the factory to retrieve.
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The result of applying [argument] to the retrieved factory.
 *
 * @throws EntryNotFoundException
 */
inline fun <reified A, reified R : Any> View.instance(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
): R = dependencyGraph.instance(argument, qualifier, generics)

/**
 * Retrieve an optional instance of [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return An instance of [R] or null if provider doesn't exist.
 */
inline fun <reified R : Any> View.instanceOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
): R? = dependencyGraph.instanceOrNull(qualifier, generics)

/**
 * Retrieve an optional factory of type `(A) -> R` and apply [argument] to it.
 *
 * @param argument The argument for the factory to retrieve.
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The result of applying [argument] to the retrieved factory or null if factory doesn't exist.
 *
 */
inline fun <reified A, reified R : Any> View.instanceOrNull(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
): R? = dependencyGraph.instanceOrNull(argument, qualifier, generics)

/**
 * Create a [Lazy] that retrieves an instance of `R` when initialized.
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The created [Lazy] instance.
 */
inline fun <reified R : Any> View.lazyInstance(
        qualifier: Any? = null,
        generics: Boolean = false
): Lazy<R> = lazy { dependencyGraph.instance<R>(qualifier, generics) }

/**
 * Create a [Lazy] that retrieves a factory of type `(A) -> R` and applies [argument]
 * to it when initialized.
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The created [Lazy] instance.
 */
inline fun <reified A, reified R : Any> View.lazyInstance(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
): Lazy<R> = lazy { dependencyGraph.instance<A, R>(argument, qualifier, generics) }

/**
 * Create a [Lazy] that retrieves an optional instance of `R` when initialized.
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The created [Lazy] instance.
 */
inline fun <reified R : Any> View.lazyInstanceOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
): Lazy<R?> = lazy { dependencyGraph.instanceOrNull<R>(qualifier, generics) }

/**
 * Create a [Lazy] that retrieves an optional factory of type `(A) -> R` and 
 * applies [argument] to it when initialized.
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The created [Lazy] instance.
 */
inline fun <reified A, reified R : Any> View.lazyInstanceOrNull(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
): Lazy<R?> = lazy { dependencyGraph.instanceOrNull<A, R>(argument, qualifier, generics) }

/**
 * Retrieve a non-optional provider function that returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The provider that returns [R]
 *
 * @throws EntryNotFoundException
 */
inline fun <reified R : Any> View.provider(
        qualifier: Any? = null, 
        generics: Boolean = false
): Provider<R> = dependencyGraph.provider(qualifier, generics)

/**
 * Retrieve a factory of type `(A) -> R` and create and return a
 * [provider][Provider] that applies the given [argument] to the factory when called.
 *
 * @param argument The argument for the factory to retrieve.
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The provider function.
 *
 * @throws EntryNotFoundException
 */
inline fun <reified A, reified R : Any> View.provider(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
): Provider<R> = dependencyGraph.provider(argument, qualifier, generics)

/**
 * Retrieve an optional provider function that returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The provider that returns [R] or null if provider doesn't exist.
 */
inline fun <reified R : Any> View.providerOrNull(
        qualifier: Any? = null, 
        generics: Boolean = false
): Provider<R>? = dependencyGraph.providerOrNull(qualifier, generics)

/**
 * Retrieve an optional factory of type `(A) -> R` and create and return a
 * [provider][Provider] that applies the given [argument] to the factory when called.
 *
 * @param argument The argument for the factory to retrieve.
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The provider function or null if factory doesn't exist.
 */
inline fun <reified A, reified R : Any> View.providerOrNull(
        argument: A,
        qualifier: Any? = null,
        generics: Boolean = false
): Provider<R>? = dependencyGraph.providerOrNull(argument, qualifier, generics)

/**
 * Retrieve a non-optional factory function that takes an argument of type [A] and returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The factory that takes [A] and returns [R]
 *
 * @throws EntryNotFoundException
 */
inline fun <reified A : Any, reified R : Any> View.factory(
        qualifier: Any? = null, 
        generics: Boolean = false
): Factory<A, R> = dependencyGraph.factory(qualifier, generics)

/**
 * Retrieve an optional factory function that takes an argument of type [A] and returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The factory that takes [A] and returns [R] or null if factory provider doesn't exist.
 */
inline fun <reified A : Any, reified R : Any> View.factoryOrNull(
        qualifier: Any? = null, 
        generics: Boolean = false
): Factory<A, R>? = dependencyGraph.factoryOrNull(qualifier, generics)

/**
 * Retrieve all providers of type [R].
 *
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return A [Set] of providers of type `() -> R`.
 */
inline fun <reified R : Any> View.providersOfType(
        generics: Boolean = false
): Set<Provider<R>> = dependencyGraph.providersOfType(generics)

/**
 * Retrieve all instances of type [R].
 *
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return A [Set] of instances of type [R].
 */
inline fun <reified R : Any> View.instancesOfType(
        generics: Boolean = false
): Set<R> = dependencyGraph.instancesOfType(generics)
