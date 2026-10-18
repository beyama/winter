package io.jentz.winter

import io.jentz.winter.services.BoundService

// JavaScript and WebAssembly are single threaded.

internal actual typealias Lock = Any

internal actual fun newLock(owner: Any): Lock = owner

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun <T> Lock.withLock(block: () -> T): T = block()

internal actual class ServiceRegistry actual constructor() {
    private val map = HashMap<TypeKey<*>, BoundService<*>>()

    actual operator fun get(key: TypeKey<*>): BoundService<*>? = map[key]

    actual operator fun set(key: TypeKey<*>, service: BoundService<*>) {
        map[key] = service
    }

    actual val values: Collection<BoundService<*>> get() = map.values
}
