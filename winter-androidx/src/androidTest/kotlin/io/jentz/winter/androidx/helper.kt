package io.jentz.winter.androidx

import androidx.test.platform.app.InstrumentationRegistry

fun runOnMainSync(block: () -> Unit) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(block)