package io.jentz.winter

import io.jentz.winter.services.BoundService
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

internal actual typealias Lock = Any

internal actual fun newLock(owner: Any): Lock = owner

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun <T> Lock.withLock(block: () -> T): T = synchronized(this, block)

internal actual class ServiceRegistry actual constructor() {
    private val map = ConcurrentHashMap<TypeKey<*>, BoundService<*>>()

    actual operator fun get(key: TypeKey<*>): BoundService<*>? = map[key]

    actual operator fun set(key: TypeKey<*>, service: BoundService<*>) {
        map[key] = service
    }

    actual val values: Collection<BoundService<*>> get() = map.values
}

internal actual val KClass<*>.displayName: String get() = java.name

internal actual val KClass<*>.qualifierName: String? get() = qualifiedName
