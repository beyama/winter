package io.jentz.winter

import kotlin.reflect.KClass

internal actual val KClass<*>.displayName: String get() = simpleName ?: toString()

// Kotlin/JS has no qualified class names, so the simple name is made unique with a number.
// JavaScript is single threaded.
private val qualifierNames = HashMap<KClass<*>, String>()

internal actual val KClass<*>.qualifierName: String?
    get() {
        val simpleName = simpleName ?: return null
        return qualifierNames.getOrPut(this) { "$simpleName#${qualifierNames.size}" }
    }
