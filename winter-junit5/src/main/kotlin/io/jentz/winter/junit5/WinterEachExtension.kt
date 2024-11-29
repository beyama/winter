package io.jentz.winter.junit5

import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.plugin.PluginBuilderBlock
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace

/**
 * JUnit5 extension that builds and installs a [Winter] [io.jentz.winter.plugin.Plugin] with
 * [block] for each test and uninstalls it after each test.
 *
 * This is useful to extend and observe the applications dependency graph during tests.
 *
 * For more details see [io.jentz.winter.plugin.PluginBuilder].
 */
open class WinterEachExtension(
    application: WinterApplication = Winter,
    block: PluginBuilderBlock
) : AbstractWinterExtension(
    Namespace.create("io.jentz.winter.each"),
    application,
    block
), BeforeEachCallback, AfterEachCallback {

    final override fun beforeEach(context: ExtensionContext) {
        before(context)
    }

    final override fun afterEach(context: ExtensionContext) {
        after(context)
    }

}
