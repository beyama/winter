package io.jentz.winter

import io.jentz.winter.services.BoundService
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

// Reentrant (owner thread and nesting count) and parks waiting threads.
internal actual typealias Lock = SynchronizedObject

internal actual fun newLock(owner: Any): Lock = SynchronizedObject()

internal actual inline fun <T> Lock.withLock(block: () -> T): T = synchronized(this, block)

/**
 * Copy on write: a graph binds each service only once, so writes are rare compared to reads.
 */
internal actual class ServiceRegistry actual constructor() {
    @Volatile private var map: Map<TypeKey<*>, BoundService<*>> = emptyMap()

    actual operator fun get(key: TypeKey<*>): BoundService<*>? = map[key]

    actual operator fun set(key: TypeKey<*>, service: BoundService<*>) {
        map = HashMap(map).apply { put(key, service) }
    }

    actual val services: Collection<BoundService<*>> get() = map.values
}

internal actual val KClass<*>.displayName: String get() = qualifiedName ?: simpleName ?: toString()

internal actual val KClass<*>.qualifierName: String? get() = qualifiedName

@PublishedApi
internal actual inline fun <reified R : Any?> classToken(): Any = R::class

@PublishedApi
internal actual val KClass<*>.classToken: Any get() = this

@PublishedApi
internal actual fun classOfToken(token: Any): KClass<*> = token as KClass<*>
