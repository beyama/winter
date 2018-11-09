package io.jentz.winter.android.test

inline fun waitForIt(intervalMs: Long = 50, timeoutMs: Long = 1000, fn: () -> Boolean) {
    require(intervalMs > 0) { "intervalMs must be greater than 0." }
    require(timeoutMs > 0) { "timeoutMs mut be greater than 0." }

    var elapsedTime = 0L
    do {
        if (fn()) return
        Thread.sleep(intervalMs)
        elapsedTime += intervalMs
    } while (elapsedTime < timeoutMs)

    throw AssertionError("Wait for it timeout.")
}