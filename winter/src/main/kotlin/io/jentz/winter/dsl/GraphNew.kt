package io.jentz.winter.dsl

import io.jentz.winter.Graph

/**
* Create an instance of type [R]. 
* All arguments will be resolved from this graph.
*
* This uses inline function an no reflection.
* Inspired by https://insert-koin.io .
*/
inline fun <reified R: Any> Graph.new(noinline constructor: () -> R): R =
    constructor()

inline fun <reified R: Any, reified A0> Graph.new(noinline constructor: (a0: A0) -> R): R =
    constructor(instance())

inline fun <reified R: Any, reified A0, reified A1> Graph.new(noinline constructor: (a0: A0, a1: A1) -> R): R =
    constructor(instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2) -> R): R =
    constructor(instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3) -> R): R =
    constructor(instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16, reified A17> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16, reified A17, reified A18> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16, reified A17, reified A18, reified A19> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

inline fun <reified R: Any, reified A0, reified A1, reified A2, reified A3, reified A4, reified A5, reified A6, reified A7, reified A8, reified A9, reified A10, reified A11, reified A12, reified A13, reified A14, reified A15, reified A16, reified A17, reified A18, reified A19, reified A20> Graph.new(noinline constructor: (a0: A0, a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20) -> R): R =
    constructor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())

