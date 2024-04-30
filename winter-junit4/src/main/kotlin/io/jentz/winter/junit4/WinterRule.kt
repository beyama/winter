package io.jentz.winter.junit4

import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.plugin.PluginBuilderBlock
import io.jentz.winter.plugin.plugin
import org.junit.rules.ExternalResource

/**
 * JUnit4 rule that builds and installs a [Winter] [io.jentz.winter.plugin.Plugin] with
 * [block] for each test and uninstalls it after each test.
 *
 * This is useful to extend and observe the applications dependency graph during tests.
 *
 * For more details see [io.jentz.winter.plugin.PluginBuilder].
 */
class WinterRule(
    private val application: WinterApplication = Winter,
    private val block: PluginBuilderBlock
) : ExternalResource() {

    private var uninstaller: (() -> Unit)? = null

    override fun before() {
        uninstaller = application.plugin(block)
    }

    override fun after() {
        uninstaller?.invoke()
        uninstaller = null
    }

}
