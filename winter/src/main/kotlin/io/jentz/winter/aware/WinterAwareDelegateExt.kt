package io.jentz.winter.aware

import io.jentz.winter.Provider
import io.jentz.winter.delegate.*
import io.jentz.winter.typeKey
import io.jentz.winter.typeKeyOfType

/**
 * Creates a property delegate for a [Provider] of type `() -> R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> WinterAware.injectProvider(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<Provider<R>> = ProviderProperty(winterApplication, typeKey(qualifier, generics))

/**
 * Creates a property delegate for an optional [Provider] of type `R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> WinterAware.injectProviderOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<Provider<R>?> =
    ProviderOrNullProperty(winterApplication, typeKey(qualifier, generics))

/**
 * Creates a property delegate for an instance of type `R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> WinterAware.inject(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<R> = InstanceProperty(winterApplication, typeKey(qualifier, generics))

/**
 * Creates a property delegate for an optional instance of type `R`.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> WinterAware.injectOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<R?> = InstanceOrNullProperty(winterApplication, typeKey(qualifier, generics))

/**
 * Creates a lazy property delegate for an instance of type `R`.
 *
 * The instance gets retrieved/created on first property access.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> WinterAware.injectLazy(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<R> = LazyInstanceProperty(winterApplication, typeKey(qualifier, generics))

/**
 * Creates a lazy property delegate for an optional instance of type `R`.
 *
 * The instance gets retrieved/created on first property access.
 *
 * @param qualifier An optional qualifier.
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> WinterAware.injectLazyOrNull(
    qualifier: Any? = null,
    generics: Boolean = false
): InjectedProperty<R?> =
    LazyInstanceOrNullProperty(winterApplication, typeKey(qualifier, generics))

/**
 * Creates a property delegate for a [Set] of [providers][Provider] of type `R`.
 *
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> WinterAware.injectProvidersOfType(
    generics: Boolean = false
): InjectedProperty<Set<Provider<R>>> =
    ProvidersOfTypeProperty(winterApplication, typeKeyOfType(generics))

/**
 * Creates a property delegate for a [Set] of instances of type `R`.
 *
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> WinterAware.injectInstancesOfType(
    generics: Boolean = false
): InjectedProperty<Set<R>> = InstancesOfTypeProperty(winterApplication, typeKeyOfType(generics))

/**
 * Creates a lazy property delegate for a [Set] of instances of type `R`.
 *
 * The instances get retrieved/created on first property access.
 *
 * @param generics Preserve generic type parameters.
 * @return The created [InjectedProperty].
 */
inline fun <reified R : Any> WinterAware.injectLazyInstancesOfType(
    generics: Boolean = false
): InjectedProperty<Set<R>> =
    LazyInstancesOfTypeProperty(winterApplication, typeKeyOfType(generics))
