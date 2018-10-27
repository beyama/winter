package io.jentz.winter.junit4

import io.jentz.winter.GraphDisposePlugin
import io.jentz.winter.InitializingComponentPlugin
import io.jentz.winter.PostConstructPlugin
import io.jentz.winter.WinterPlugins
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit 4 [test rule][TestRule] that allows to hook into the Component/Graph lifecycle.
 *
 * This registers given Winter plugins on [WinterPlugins] before executing a test and removes
 * them after the test.
 *
 * Sample where we replace a view model in a presentation graph with a test view model:
 * ```
 * val testViewModel = TestViewModel<ViewState>()
 *
 * @get:Rule
 * val winterTestRule = WinterTestRule.initializingComponent { _, builder ->
 *   if (builder.qualifier == "presentation") {
 *     builder.apply {
 *       singleton<ViewModel<ViewState>>(generics = true, override = true) { testViewModel }
 *     }
 *   }
 * }
 * ```
 *
 */
class WinterTestRule(
    private val initializingComponentPlugin: InitializingComponentPlugin? = null,
    private val postConstructPlugin: PostConstructPlugin? = null,
    private val graphDisposePlugin: GraphDisposePlugin? = null
) : TestRule {

    companion object {
        /**
         * Creates a [WinterTestRule] with a [InitializingComponentPlugin].
         */
        @JvmStatic
        fun initializingComponent(plugin: InitializingComponentPlugin) =
            WinterTestRule(initializingComponentPlugin = plugin)

        /**
         * Creates a [WinterTestRule] with a [PostConstructPlugin].
         */
        @JvmStatic
        fun postConstruct(plugin: PostConstructPlugin) =
            WinterTestRule(postConstructPlugin = plugin)

        /**
         * Creates a [WinterTestRule] with a [GraphDisposePlugin].
         */
        @JvmStatic
        fun graphDispose(plugin: GraphDisposePlugin) =
            WinterTestRule(graphDisposePlugin = plugin)
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                if (initializingComponentPlugin != null) {
                    WinterPlugins.addInitializingComponentPlugin(initializingComponentPlugin)
                }
                if (postConstructPlugin != null) {
                    WinterPlugins.addPostConstructPlugin(postConstructPlugin)
                }
                if (graphDisposePlugin != null) {
                    WinterPlugins.addGraphDisposePlugin(graphDisposePlugin)
                }
                try {
                    base.evaluate()
                } finally {
                    if (initializingComponentPlugin != null) {
                        WinterPlugins.removeInitializingComponentPlugin(initializingComponentPlugin)
                    }
                    if (postConstructPlugin != null) {
                        WinterPlugins.removePostConstructPlugin(postConstructPlugin)
                    }
                    if (graphDisposePlugin != null) {
                        WinterPlugins.removeGraphDisposePlugin(graphDisposePlugin)
                    }
                }
            }
        }
}
