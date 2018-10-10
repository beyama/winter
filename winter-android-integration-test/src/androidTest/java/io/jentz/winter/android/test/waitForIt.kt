package io.jentz.winter.android.test

inline fun waitForIt(intervalMs: Long = 50, timeoutMs: Long = 1000, fn: () -> Boolean) {
    if (fn()) return

    var elapsedTime = 0L
    while (elapsedTime < timeoutMs) {
        Thread.sleep(intervalMs)
        elapsedTime += intervalMs
        if (fn()) return
    }
    throw AssertionError("Wait for it timeout.")
}