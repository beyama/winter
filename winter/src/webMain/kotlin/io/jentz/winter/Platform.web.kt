package io.jentz.winter

import io.jentz.winter.services.BoundService
import kotlin.reflect.KClass

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

    actual val services: Collection<BoundService<*>> get() = map.values
}

@PublishedApi
internal actual inline fun <reified R : Any?> classToken(): Any = R::class

@PublishedApi
internal actual val KClass<*>.classToken: Any get() = this

@PublishedApi
internal actual fun classOfToken(token: Any): KClass<*> = token as KClass<*>
