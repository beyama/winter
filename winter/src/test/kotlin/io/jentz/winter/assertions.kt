package io.jentz.winter

import org.opentest4j.AssertionFailedError

inline fun <T> expectValueToChange(from: T, to: T, valueProvider: () -> T, block: () -> Unit) {
    val a = valueProvider()
    if (a != from) fail("Expected initial value to be ${formatValue(from)} but was ${formatValue(a)}")
    block()
    val b = valueProvider()
    if (b != to) fail("Expected change from ${formatValue(from)} to ${formatValue(to)} but was ${formatValue(b)}")
}

internal inline fun <reified S : UnboundService<*>> Component.shouldContainServiceOfType(key: TypeKey<*>) {
    val service = this[key]
            ?: fail("Component was expected to contain service with key <$key> but doesn't")
    if (service !is S) fail("Service with key <$key> was expected to be <${S::class}> but was <${service.javaClass}>.")
}

internal fun Component.shouldContainService(key: TypeKey<*>) {
    if (!containsKey(key)) fail("Component was expected to contain service with key <$key> but doesn't")
}

internal fun Component.shouldNotContainService(key: TypeKey<*>) {
    if (containsKey(key)) fail("Component wasn't expected to contain service with key <$key> but does.")
}

fun fail(message: String): Nothing {
    throw AssertionFailedError(message)
}

@PublishedApi
internal fun formatValue(any: Any?) = when (any) {
    is String -> "\"$any\""
    else -> "<$any>"
}