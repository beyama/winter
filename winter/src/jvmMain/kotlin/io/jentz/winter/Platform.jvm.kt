package io.jentz.winter

import io.jentz.winter.services.BoundService
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

internal actual typealias Lock = Any

internal actual fun newLock(owner: Any): Lock = owner

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun <T> Lock.withLock(block: () -> T): T = synchronized(this, block)

// The registry is the map itself (no wrapper object), saves a dependent load per lookup.
internal actual class ServiceRegistry actual constructor() :
    ConcurrentHashMap<TypeKey<*>, BoundService<*>>() {

    actual override operator fun get(key: TypeKey<*>): BoundService<*>? = super.get(key)

    actual operator fun set(key: TypeKey<*>, service: BoundService<*>) {
        put(key, service)
    }

    actual val services: Collection<BoundService<*>> get() = super.values
}

internal actual val KClass<*>.displayName: String get() = java.name

internal actual val KClass<*>.qualifierName: String? get() = qualifiedName

// R::class.java of a reified type parameter is the object type (java.lang.Integer for Int) and
// compiles to a class constant.
@PublishedApi
internal actual inline fun <reified R : Any?> classToken(): Any = R::class.java

@PublishedApi
internal actual val KClass<*>.classToken: Any get() = javaObjectType

@PublishedApi
internal actual fun classOfToken(token: Any): KClass<*> = (token as Class<*>).kotlin
