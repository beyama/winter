package io.jentz.winter.compiler

import java.util.*

// Used to set a fixed date for testing
internal var currentDateFixed: Date? = null

fun now(): Date = currentDateFixed ?: Date()
