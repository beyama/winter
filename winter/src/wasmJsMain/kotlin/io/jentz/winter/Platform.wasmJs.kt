package io.jentz.winter

import kotlin.reflect.KClass

internal actual val KClass<*>.displayName: String get() = qualifiedName ?: simpleName ?: toString()

internal actual val KClass<*>.qualifierName: String? get() = qualifiedName
