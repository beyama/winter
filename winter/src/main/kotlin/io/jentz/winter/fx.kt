package io.jentz.winter

internal object UNINITIALIZED_VALUE

fun <T> memorize(fn: () -> T): () -> T {
    var value: Any? = UNINITIALIZED_VALUE

    return fun(): T {
        val v1 = value
        if (v1 !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        synchronized(fn) {
            val v2 = value
            if (value !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return v2 as T
            }

            val typedValue = fn()
            value = typedValue
            return typedValue
        }
    }

}
