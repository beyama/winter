package io.jentz.winter.junit5

import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.plugin.SimplePlugin
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit5 extension that allows to hook into the [io.jentz.winter.Graph] lifecycle.
 *
 * This registers itself as Winter plugin on [application] before executing a test and unregisters
 * itself afterwards.
 */
abstract class GraphLifecycleExtension(
    private val application: WinterApplication = Winter
): SimplePlugin(), BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        application.registerPlugin(this)
    }

    override fun afterEach(context: ExtensionContext) {
        application.unregisterPlugin(this)
    }
}
