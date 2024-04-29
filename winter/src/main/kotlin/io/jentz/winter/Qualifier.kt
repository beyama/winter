package io.jentz.winter

import kotlin.reflect.KClass

@JvmInline
value class Qualifier(val value: String) {
    init {
        require(value.isNotBlank()) { "Qualifier value must not be blank" }
    }

    override fun toString(): String =
        "qualifier($value)"
}

fun qualifier(value: String) = Qualifier(value)

inline fun <reified T: Any> qualifier() = T::class.qualifier()

fun <E: Enum<E>> qualifier(value: E) = Qualifier(value.name)

fun <T: Any> KClass<T>.qualifier() = Qualifier(requireNotNull(qualifiedName) {
    "Class must not be local or from an anonymous type"
})

val <E: Enum<E>> Enum<E>.qualifier: Qualifier get() = Qualifier(this.name)