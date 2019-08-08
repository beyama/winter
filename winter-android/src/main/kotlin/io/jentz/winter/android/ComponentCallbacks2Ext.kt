/**
 * GENERATED FILE - DO NOT EDIT!
 *
 * To change the content of this file edit inject_extension.erb and
 * run 'ruby generate_inject_extensions.rb'.
 */
@file:Suppress("DEPRECATION")

package io.jentz.winter.android

import android.content.ComponentCallbacks2
import io.jentz.winter.Factory
import io.jentz.winter.Provider
import io.jentz.winter.Graph
import io.jentz.winter.Injection

/**
 * Return the graph associated with `this`.
 * This is sugar for calling "Injection.getGraph(this)".
 */
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline val ComponentCallbacks2.dependencyGraph: Graph get() = Injection.getGraph(this)

/**
 * Retrieve a non-optional instance of [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return An instance of [R]
 *
 * @throws io.jentz.winter.EntryNotFoundException
 */
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified R : Any> ComponentCallbacks2.instance(
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
 * @throws io.jentz.winter.EntryNotFoundException
 */
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified A, reified R : Any> ComponentCallbacks2.instance(
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
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified R : Any> ComponentCallbacks2.instanceOrNull(
        qualifier: Any? = null,
        generics: Boolean = false
): R? = dependencyGraph.instanceOrNull(qualifier, generics)

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
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified A, reified R : Any> ComponentCallbacks2.instanceOrNull(
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
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified R : Any> ComponentCallbacks2.lazyInstance(
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
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified A, reified R : Any> ComponentCallbacks2.lazyInstance(
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
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified R : Any> ComponentCallbacks2.lazyInstanceOrNull(
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
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified A, reified R : Any> ComponentCallbacks2.lazyInstanceOrNull(
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
 * @throws io.jentz.winter.EntryNotFoundException
 */
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified R : Any> ComponentCallbacks2.provider(
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
 * @throws io.jentz.winter.EntryNotFoundException
 */
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified A, reified R : Any> ComponentCallbacks2.provider(
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
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified R : Any> ComponentCallbacks2.providerOrNull(
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
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified A, reified R : Any> ComponentCallbacks2.providerOrNull(
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
 * @throws io.jentz.winter.EntryNotFoundException
 */
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified A : Any, reified R : Any> ComponentCallbacks2.factory(
        qualifier: Any? = null, 
        generics: Boolean = false
): Factory<A, R> = dependencyGraph.factory(qualifier, generics)

/**
 * Retrieve an optional factory function that takes an argument of type [A] and returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return The factory that takes [A] and returns [R] or null if factory provider doesn't exist.
 *
 * @throws io.jentz.winter.EntryNotFoundException
 */
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified A : Any, reified R : Any> ComponentCallbacks2.factoryOrNull(
        qualifier: Any? = null, 
        generics: Boolean = false
): Factory<A, R>? = dependencyGraph.factoryOrNull(qualifier, generics)

/**
 * Retrieve all providers of type [R].
 *
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return A [Set] of providers of type `() -> R`.
 */
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified R : Any> ComponentCallbacks2.providersOfType(
        generics: Boolean = false
): Set<Provider<R>> = dependencyGraph.providersOfType(generics)

/**
 * Retrieve all instances of type [R].
 *
 * @param generics Preserves generic type parameters if set to true (default = false).
 * @return A [Set] of instances of type [R].
 */
@Deprecated("Implement WinterAware and use its extension functions.", ReplaceWith(""))
inline fun <reified R : Any> ComponentCallbacks2.instancesOfType(
        generics: Boolean = false
): Set<R> = dependencyGraph.instancesOfType(generics)
