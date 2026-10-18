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
    val values: Collection<BoundService<*>>
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
