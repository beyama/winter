package io.jentz.winter

import org.opentest4j.AssertionFailedError

fun Any?.shouldBeNull() {
    if (this != null) fail("${formatValue(this)} was expected to be null.")
}

inline fun <T> expectValueToChange(from: T, to: T, valueProvider: () -> T, block: () -> Unit) {
    val a = valueProvider()
    if (a != from) fail("Expected initial value to be ${formatValue(from)} but was ${formatValue(a)}")
    block()
    val b = valueProvider()
    if (b != to) fail("Expected change from ${formatValue(from)} to ${formatValue(to)} but was ${formatValue(b)}")
}

fun fail(message: String): Nothing {
    throw AssertionFailedError(message)
}

@PublishedApi
internal fun formatValue(any: Any?) = when (any) {
    is String -> "\"$any\""
    else -> "<$any>"
}