package io.jentz.winter.dsl

import io.jentz.winter.Component
import io.jentz.winter.TypeKey
import io.jentz.winter.typeKey

/**
* Register a singleton scoped constructor for an instance of type [R].
*
* @param constructor The constructor of type [R].
* @param key The [TypeKey] to register this singleton.
*/
inline fun <reified R: Any> Component.Builder.singletonOf(noinline constructor: () -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0> Component.Builder.singletonOf(noinline constructor: (a0: A0) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16, reified A17> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16, reified A17, reified A18> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16, reified A17, reified A18, reified A19> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16, reified A17, reified A18, reified A19, reified A20> Component.Builder.singletonOf(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20) -> R, key: TypeKey<R> = typeKey()) =
    singleton(key) { new(constructor) }

