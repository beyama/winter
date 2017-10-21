package io.jentz.winter

import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

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

fun <T> memorizeWeak(fn: () -> T): () -> T = memorizeReferenced(::WeakReference, fn)

fun <T> memorizeSoft(fn: () -> T): () -> T = memorizeReferenced(::SoftReference, fn)

private fun <T> memorizeReferenced(refCreator: (T) -> Reference<T>, fn: () -> T): () -> T {
    var value: Any? = UNINITIALIZED_VALUE

    return fun(): T {
        val v1 = value

        if (v1 !== UNINITIALIZED_VALUE) {
            val ref = v1 as Reference<*>
            ref.get()?.let {
                @Suppress("UNCHECKED_CAST")
                return it as T
            }
        }

        synchronized(fn) {
            val v2 = value

            if (v2 !== UNINITIALIZED_VALUE) {
                val ref = v2 as WeakReference<*>
                ref.get()?.let {
                    @Suppress("UNCHECKED_CAST")
                    return it as T
                }
            }

            val typedValue = fn()
            value = refCreator(typedValue)
            return typedValue
        }
    }
}

fun <A, R> multiton(fn: (A) -> R): (A) -> R {
    val map = mutableMapOf<A, R>()

    return fun(arg: A): R {
        synchronized(map) {
            if (map.containsKey(arg)) {
                @Suppress("UNCHECKED_CAST")
                return map[arg] as R
            }

            val typedValue = fn(arg)
            map[arg] = typedValue
            return typedValue
        }
    }
}

