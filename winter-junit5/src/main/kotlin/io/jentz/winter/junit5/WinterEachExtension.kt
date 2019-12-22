package io.jentz.winter.junit5

import io.jentz.winter.testing.WinterTestSession.Builder
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace

/**
 * JUnit5 extension that starts a [io.jentz.winter.testing.WinterTestSession] before each test
 * and stops the session after each test.
 *
 * For more details see [io.jentz.winter.testing.WinterTestSession].
 */
open class WinterEachExtension(
    block: Builder.() -> Unit
) : AbstractWinterExtension(
    Namespace.create("io.jentz.winter.each"),
    Builder().apply(block)
), BeforeEachCallback, AfterEachCallback {

    /**
     * Default constructor to use this with [org.junit.jupiter.api.extension.RegisterExtension].
     *
     * The default configuration will operate on the application graph and will bind all `Mock`
     * annotated properties to it.
     *
     * This class is open and can be extended for other default configurations.
     */
    constructor(): this({ bindAllMocks() })

    final override fun beforeEach(context: ExtensionContext) {
        before(context)
    }

    final override fun afterEach(context: ExtensionContext) {
        after(context)
    }

}
