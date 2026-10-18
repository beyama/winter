package io.jentz.winter

import io.jentz.winter.services.BoundService
import kotlin.reflect.KClass

/**
 * A reentrant lock.
 *
 * On the JVM this is the monitor of the owner passed to [newLock], so the lock order and the lock
 * behaviour are the same as with `synchronized(owner)`. JavaScript and WebAssembly are single
 * threaded and don't lock at all.
 */
internal expect class Lock

internal expect fun newLock(owner: Any): Lock

internal expect inline fun <T> Lock.withLock(block: () -> T): T

/**
 * Registry of bound services of a graph.
 *
 * Reads are lock-free and may happen concurrently to writes, writes only happen under the graph
 * lock.
 */
internal expect class ServiceRegistry() {
    operator fun get(key: TypeKey<*>): BoundService<*>?
    operator fun set(key: TypeKey<*>, service: BoundService<*>)
    val services: Collection<BoundService<*>>
}

/**
 * The name of a class for messages: the binary name on the JVM, the qualified name where
 * available and otherwise the simple name.
 */
internal expect val KClass<*>.displayName: String

/**
 * A name that identifies the class, used by [qualifier] with a class. Null for local and anonymous
 * classes.
 */
internal expect val KClass<*>.qualifierName: String?

/**
 * Identity of the class [R] for [ClassTypeKey]: the `java.lang.Class` (object type) on the JVM,
 * so that [erased] doesn't allocate a `KClass` per key, and the [KClass] elsewhere.
 * Its `hashCode` must be equal to the hash code of the [KClass].
 */
@PublishedApi
internal expect inline fun <reified R : Any?> classToken(): Any

@PublishedApi
internal expect val KClass<*>.classToken: Any

@PublishedApi
internal expect fun classOfToken(token: Any): KClass<*>
