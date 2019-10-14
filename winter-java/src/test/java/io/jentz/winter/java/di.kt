@file:JvmName("Di")

package io.jentz.winter.java

import io.jentz.winter.component

val testComponent = component {

    prototype { "prototype" }
    prototype("a") { "prototype a" }
    prototype("b") { "prototype b" }
    prototype("c") { "prototype c" }

    factory { i: Int -> i.toString() }

}
