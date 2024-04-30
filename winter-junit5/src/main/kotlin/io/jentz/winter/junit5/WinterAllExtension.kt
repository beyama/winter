package io.jentz.winter.junit5

import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.plugin.PluginBuilderBlock
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace

/**
 * JUnit5 extension that builds and installs a [Winter] [io.jentz.winter.plugin.Plugin] with
 * [block] before all tests and uninstalls it after all tests.
 *
 * For more details see [io.jentz.winter.plugin.PluginBuilder].
 */
open class WinterAllExtension(
    application: WinterApplication = Winter,
    block: PluginBuilderBlock
) : AbstractWinterExtension(
    Namespace.create("io.jentz.winter.all"),
    application,
    block
), BeforeAllCallback, AfterAllCallback {

    final override fun beforeAll(context: ExtensionContext) {
        before(context)
    }

    final override fun afterAll(context: ExtensionContext) {
        after(context)
    }

}
