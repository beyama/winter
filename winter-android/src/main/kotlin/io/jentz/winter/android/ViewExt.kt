package io.jentz.winter.android

import android.view.View
import io.jentz.winter.EntryNotFoundException
import io.jentz.winter.Graph

/**
 * Returns the activity graph.
 */
inline val View.graph: Graph get() = AndroidInjection.getGraph(this)

/**
 * Retrieve a non-optional instance of [T].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserve generic type parameters.
 * @return An instance of [T]
 *
 * @throws EntryNotFoundException
 */
inline fun <reified T : Any> View.instance(qualifier: Any? = null, generics: Boolean = false): T =
        graph.instance(qualifier, generics)

/**
 * Retrieve an optional instance of [T].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserve generic type parameters.
 * @return An instance of [T] or null if provider doesn't exist.
 */
inline fun <reified T : Any> View.instanceOrNull(qualifier: Any? = null, generics: Boolean = false): T? =
        graph.instanceOrNull(qualifier, generics)

/**
 * Retrieve a non-optional provider function that returns [T].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserve generic type parameters.
 * @return The provider that returns [T]
 *
 * @throws EntryNotFoundException
 */
inline fun <reified T : Any> View.provider(qualifier: Any? = null, generics: Boolean = false): () -> T =
        graph.provider(qualifier, generics)

/**
 * Retrieve an optional provider function that returns [T].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserve generic type parameters.
 * @return The provider that returns [T] or null if provider doesn't exist.
 */
inline fun <reified T : Any> View.providerOrNull(qualifier: Any? = null, generics: Boolean = false): (() -> T)? =
        graph.providerOrNull(qualifier, generics)


/**
 * Retrieve a non-optional factory function that takes an argument of type [A] and returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserve generic type parameters.
 * @return The factory that takes [A] and returns [R]
 *
 * @throws EntryNotFoundException
 */
inline fun <reified A : Any, reified R : Any> View.factory(qualifier: Any? = null, generics: Boolean = false): (A) -> R =
        graph.factory(qualifier, generics)

/**
 * Retrieve an optional factory function that takes an argument of type [A] and returns [R].
 *
 * @param qualifier An optional qualifier of the dependency.
 * @param generics Preserve generic type parameters.
 * @return The factory that takes [A] and returns [R] or null if factory provider doesn't exist.
 *
 * @throws EntryNotFoundException
 */
inline fun <reified A : Any, reified R : Any> View.factoryOrNull(qualifier: Any? = null, generics: Boolean = false): ((A) -> R)? =
        graph.factoryOrNull(qualifier, generics)

/**
 * Retrieve all providers of type `T`.
 *
 * @param generics Preserve generic type parameters.
 * @return A [Set] of providers of type `() -> T`.
 */
inline fun <reified T : Any> View.providersOfType(generics: Boolean = false): Set<() -> T> =
        graph.providersOfType(generics)

/**
 * Retrieve all instances of type [T].
 *
 * @param generics Preserve generic type parameters.
 * @return A [Set] of instances of type `T`.
 */
inline fun <reified T : Any> View.instancesOfType(generics: Boolean = false): Set<T> =
        graph.instancesOfType(generics)