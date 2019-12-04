package io.jentz.winter.junit4

import io.jentz.winter.Winter
import io.jentz.winter.WinterApplication
import io.jentz.winter.plugin.SimplePlugin
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit4 [test rule][TestRule] that allows to hook into the [io.jentz.winter.Graph] lifecycle.
 *
 * This registers itself as Winter plugin on [application] before executing a test and unregisters
 * itself afterwards.
 *
 * Sample where we replace a view model in a presentation graph with a test view model:
 * ```
 * val testViewModel = TestViewModel<ViewState>()
 *
 * @get:Rule
 * val winterTestRule = object : GraphLifecycleRule() {
 *   override fun initializingGraph(parentGraph: Graph?, builder: ComponentBuilder) {
 *     if (builder.qualifier == "presentation") {
 *       builder.apply {
 *         singleton<ViewModel<QuotesViewState>>(generics = true, override = true) {
 *           viewModel
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 */
abstract class GraphLifecycleRule(
    private val application: WinterApplication = Winter
) : SimplePlugin(), TestRule {

    final override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                application.registerPlugin(this@GraphLifecycleRule)
                try {
                    base.evaluate()
                } finally {
                    application.unregisterPlugin(this@GraphLifecycleRule)
                }
            }
        }
    }

}
