package io.jentz.winter.delegate

internal actual class NotInjectedException actual constructor(
    message: String?
) : RuntimeException(message)
