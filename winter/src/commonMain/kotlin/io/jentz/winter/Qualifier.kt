package io.jentz.winter

import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

@JvmInline
value class Qualifier internal constructor(val value: String) {
    init {
        require(value.isNotBlank()) { "Qualifier value must not be blank" }
    }

    override fun toString(): String =
        "qualifier($value)"
}

fun qualifier(value: String) = Qualifier(value)

inline fun <reified T: Any> qualifier() = T::class.qualifier()

fun <E: Enum<E>> qualifier(value: E) = Qualifier(value.name)

/**
 * Returns a [Qualifier] for this class: its qualified name on the JVM, native and WebAssembly
 * targets and a name that is unique for the running program on JavaScript, which doesn't have
 * qualified class names.
 */
fun <T: Any> KClass<T>.qualifier() = Qualifier(requireNotNull(qualifierName) {
    "Class must not be local or from an anonymous type"
})

val <E: Enum<E>> Enum<E>.qualifier: Qualifier get() = Qualifier(this.name)